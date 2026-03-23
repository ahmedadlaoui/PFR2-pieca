package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.AuthResponse;
import com.pieca.backend.domain.dtos.LoginRequest;
import com.pieca.backend.domain.dtos.RegisterBuyerRequest;
import com.pieca.backend.domain.dtos.RegisterSellerRequest;
import com.pieca.backend.security.CustomUserDetails;
import com.pieca.backend.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/buyer")
    public ResponseEntity<AuthResponse> registerBuyer(@Valid @RequestBody RegisterBuyerRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.registerBuyer(request);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponse> registerSeller(@Valid @RequestBody RegisterSellerRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.registerSeller(request);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
            
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        AuthResponse response = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken()); // Refresh token rotation if applicable
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Should be true in production when HTTPS is configured
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0); // Immediately expire the cookie
        httpResponse.addCookie(cookie);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AuthResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
    
    // --- Helper ---
    
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // MUST be true in production with HTTPS
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds
        response.addCookie(cookie);
    }
}
