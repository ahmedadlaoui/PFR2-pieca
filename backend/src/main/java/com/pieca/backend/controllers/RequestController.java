package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.BuyerRequestItemResponse;
import com.pieca.backend.domain.dtos.CreateRequestRequest;
import com.pieca.backend.domain.dtos.CreateRequestResponse;
import com.pieca.backend.domain.dtos.SellerDashboardStatsResponse;
import com.pieca.backend.domain.dtos.SellerRequestItemResponse;
import com.pieca.backend.domain.enums.OfferStatus;
import com.pieca.backend.domain.enums.RequestStatus;
import com.pieca.backend.exceptions.InvalidCredentialsException;
import com.pieca.backend.services.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateRequestResponse> createRequest(
            @Valid CreateRequestRequest request,
            @RequestParam(required = false) MultipartFile photo,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour creer une demande");
        }

        CreateRequestResponse created = requestService.createBuyerRequest(request, authentication.getName(), photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<BuyerRequestItemResponse>> getMyRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour consulter vos demandes");
        }

        Page<BuyerRequestItemResponse> response = requestService.getBuyerRequests(authentication.getName(), status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse> getRequestDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour voir cette demande");
        }

        com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse response = requestService.getBuyerRequestDetails(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photos/{fileName:.+}")
    public ResponseEntity<Resource> getRequestPhoto(@PathVariable String fileName) {
        Resource file = requestService.loadRequestPhoto(fileName);
        MediaType mediaType = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(file);
    }

    @GetMapping("/nearby")
    public ResponseEntity<Page<BuyerRequestItemResponse>> getNearbyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour voir les demandes à proximité");
        }

        Page<BuyerRequestItemResponse> response = requestService.getNearbyRequests(authentication.getName(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour accepter cette demande");
        }
        requestService.acceptRequest(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accepted")
    public ResponseEntity<Page<BuyerRequestItemResponse>> getAcceptedRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise");
        }
        Page<BuyerRequestItemResponse> response = requestService.getAcceptedRequests(authentication.getName(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/offers")
    public ResponseEntity<Page<SellerRequestItemResponse>> getSellerOffers(
            @RequestParam(required = false) OfferStatus offerStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise");
        }
        Page<SellerRequestItemResponse> response = requestService.getSellerOffers(authentication.getName(), offerStatus, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/stats")
    public ResponseEntity<SellerDashboardStatsResponse> getSellerStats(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise");
        }
        SellerDashboardStatsResponse response = requestService.getSellerStats(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOffer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidCredentialsException("Authentification requise pour annuler cette offre");
        }
        requestService.cancelOffer(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

}
