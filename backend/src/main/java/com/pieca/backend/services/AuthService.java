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
        ensurePhoneNotTaken(request.getPhoneNumber());

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.BUYER)
                .build();

        userRepository.save(user);
        log.info("Buyer registered successfully: {}", user.getPhoneNumber());

        String token = jwtService.generateToken(new CustomUserDetails(user));

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Transactional
    public AuthResponse registerSeller(RegisterSellerRequest request) {
        ensurePhoneNotTaken(request.getPhoneNumber());

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .build();

        List<Category> categories = request.getCategoryIds().stream()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("Category not found with id: {}", id);
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
        log.info("Seller registered successfully: {}", user.getPhoneNumber());

        String token = jwtService.generateToken(new CustomUserDetails(user));

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> {
                    log.error("Login failed: no account found for phone {}", request.getPhoneNumber());
                    return new InvalidCredentialsException("Aucun compte trouvé avec ce numéro de téléphone");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Login failed: incorrect password for phone {}", request.getPhoneNumber());
            throw new InvalidCredentialsException("Mot de passe incorrect");
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));
        log.info("User logged in successfully: {}", user.getPhoneNumber());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private void ensurePhoneNotTaken(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.error("Registration conflict: phone number already in use: {}", phoneNumber);
            throw new ConflictException("Ce numéro de téléphone est déjà utilisé");
        }
    }
}
