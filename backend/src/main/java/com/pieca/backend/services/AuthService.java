package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.AuthResponse;
import com.pieca.backend.domain.dtos.LoginRequest;
import com.pieca.backend.domain.dtos.RegisterBuyerRequest;
import com.pieca.backend.domain.dtos.RegisterSellerRequest;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.SellerProfile;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.Role;
import com.pieca.backend.exceptions.ConflictException;
import com.pieca.backend.exceptions.InvalidCredentialsException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.repositories.CategoryRepository;
import com.pieca.backend.repositories.UserRepository;
import com.pieca.backend.security.CustomUserDetails;
import com.pieca.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public AuthResponse registerBuyer(RegisterBuyerRequest request) {
        ensureEmailNotTaken(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.BUYER)
                .build();

        userRepository.save(user);
        log.info("Buyer registered successfully: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerSeller(RegisterSellerRequest request) {
        ensureEmailNotTaken(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .build();

        List<Category> categories = request.getCategoryIds().stream()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("Catégorie introuvable avec l'id: {}", id);
                            return new ResourceNotFoundException("Catégorie introuvable avec l'id: " + id);
                        }))
                .toList();

        SellerProfile.SellerProfileBuilder profileBuilder = SellerProfile.builder()
                .user(user)
                .sellerType(request.getSellerType())
                .categories(categories);

        if (request.getLatitude() != null && request.getLongitude() != null) {
            profileBuilder.location(
                    GEOMETRY_FACTORY.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()))
            );
        }

        if (request.getActiveRadiusKm() != null) {
            profileBuilder.activeRadiusKm(request.getActiveRadiusKm());
        }

        if (request.getCustomCategoryNote() != null) {
            profileBuilder.customCategoryNote(request.getCustomCategoryNote());
        }

        user.setSellerProfile(profileBuilder.build());
        userRepository.save(user);
        log.info("Seller registered successfully: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Login failed: no account found for email {}", request.getEmail());
                    return new InvalidCredentialsException("Aucun compte trouvé avec cet email");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Login failed: incorrect password for email {}", request.getEmail());
            throw new InvalidCredentialsException("Mot de passe incorrect");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Refresh token failed: user not found for email {}", email);
                    return new InvalidCredentialsException("Token de rafraîchissement invalide");
                });

        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            log.error("Refresh token failed: token expired or invalid for email {}", email);
            throw new InvalidCredentialsException("Token de rafraîchissement expiré ou invalide");
        }

        String newAccessToken = jwtService.generateToken(userDetails);
        log.info("Access token refreshed for: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found for /me endpoint: {}", email);
                    return new ResourceNotFoundException("Utilisateur introuvable");
                });

        return AuthResponse.builder()
                .role(user.getRole())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("firstName", user.getFirstName());
        extraClaims.put("lastName", user.getLastName());
        extraClaims.put("profileImageUrl", user.getProfileImageUrl());
        
        String accessToken = jwtService.generateToken(extraClaims, userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private void ensureEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            log.error("Registration conflict: email already in use: {}", email);
            throw new ConflictException("Cet email est déjà utilisé");
        }
    }
}
