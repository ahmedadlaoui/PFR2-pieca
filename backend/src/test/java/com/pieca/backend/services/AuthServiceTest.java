package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.AuthResponse;
import com.pieca.backend.domain.dtos.LoginRequest;
import com.pieca.backend.domain.dtos.RegisterBuyerRequest;
import com.pieca.backend.domain.dtos.RegisterSellerRequest;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.Role;
import com.pieca.backend.domain.enums.SellerType;
import com.pieca.backend.exceptions.ConflictException;
import com.pieca.backend.exceptions.InvalidCredentialsException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.repositories.CategoryRepository;
import com.pieca.backend.repositories.UserRepository;
import com.pieca.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private User savedBuyer() {
        return User.builder()
                .id(1L).email("buyer@test.ma")
                .firstName("Ali").lastName("Benali")
                .passwordHash("hashed").role(Role.BUYER).build();
    }

    private void stubTokenGeneration() {
        when(jwtService.generateToken(anyMap(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
    }

    @Test
    void registerBuyer_returnsAuthResponse_withBuyerRole() {
        RegisterBuyerRequest req = new RegisterBuyerRequest();
        req.setEmail("buyer@test.ma");
        req.setPassword("secret");
        req.setFirstName("Ali");
        req.setLastName("Benali");

        when(userRepository.existsByEmail("buyer@test.ma")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedBuyer());
        stubTokenGeneration();

        AuthResponse response = authService.registerBuyer(req);

        assertThat(response.getRole()).isEqualTo(Role.BUYER);
        assertThat(response.getEmail()).isEqualTo("buyer@test.ma");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerBuyer_throws_whenEmailAlreadyTaken() {
        RegisterBuyerRequest req = new RegisterBuyerRequest();
        req.setEmail("taken@test.ma");
        req.setPassword("secret");

        when(userRepository.existsByEmail("taken@test.ma")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerBuyer(req))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerSeller_returnsAuthResponse_withSellerRole() {
        RegisterSellerRequest req = new RegisterSellerRequest();
        req.setEmail("seller@test.ma");
        req.setPassword("secret");
        req.setFirstName("Omar");
        req.setLastName("Alami");
        req.setSellerType(SellerType.LOCAL_STORE);
        req.setCategoryIds(List.of(1L));

        Category cat = Category.builder().id(1L).name("Electronique").isActive(true).build();
        User savedSeller = User.builder()
                .id(2L).email("seller@test.ma").role(Role.SELLER)
                .firstName("Omar").lastName("Alami").passwordHash("hashed").build();

        when(userRepository.existsByEmail("seller@test.ma")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(userRepository.save(any(User.class))).thenReturn(savedSeller);
        stubTokenGeneration();

        AuthResponse response = authService.registerSeller(req);

        assertThat(response.getRole()).isEqualTo(Role.SELLER);
        assertThat(response.getEmail()).isEqualTo("seller@test.ma");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerSeller_throws_whenCategoryNotFound() {
        RegisterSellerRequest req = new RegisterSellerRequest();
        req.setEmail("seller@test.ma");
        req.setPassword("secret");
        req.setSellerType(SellerType.LOCAL_STORE);
        req.setCategoryIds(List.of(99L));

        when(userRepository.existsByEmail("seller@test.ma")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerSeller(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_returnsTokens_whenCredentialsAreValid() {
        LoginRequest req = new LoginRequest();
        req.setEmail("buyer@test.ma");
        req.setPassword("secret");

        User user = savedBuyer();
        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        stubTokenGeneration();

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_throws_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.ma");
        req.setPassword("secret");

        when(userRepository.findByEmail("ghost@test.ma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throws_whenPasswordIsWrong() {
        LoginRequest req = new LoginRequest();
        req.setEmail("buyer@test.ma");
        req.setPassword("wrong");

        User user = savedBuyer();
        when(userRepository.findByEmail("buyer@test.ma")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
