package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.CreateRequestRequest;
import com.pieca.backend.domain.dtos.CreateRequestResponse;
import com.pieca.backend.exceptions.InvalidCredentialsException;
import com.pieca.backend.services.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<CreateRequestResponse> createRequest(
            @Valid @RequestBody CreateRequestRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour publier une demande");
        }

        CreateRequestResponse created = requestService.createBuyerRequest(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
