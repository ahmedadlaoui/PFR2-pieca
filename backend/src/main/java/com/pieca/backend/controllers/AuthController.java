package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.AuthResponse;
import com.pieca.backend.domain.dtos.LoginRequest;
import com.pieca.backend.domain.dtos.RegisterBuyerRequest;
import com.pieca.backend.domain.dtos.RegisterSellerRequest;
import com.pieca.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/buyer")
    public ResponseEntity<AuthResponse> registerBuyer(@Valid @RequestBody RegisterBuyerRequest request) {
        AuthResponse response = authService.registerBuyer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponse> registerSeller(@Valid @RequestBody RegisterSellerRequest request) {
        AuthResponse response = authService.registerSeller(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
