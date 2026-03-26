package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.SellerNearbyResponse;
import com.pieca.backend.security.CustomUserDetails;
import com.pieca.backend.services.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * Returns all sellers whose service coverage circle contains the provided coordinates.
     * Used by the search overlay to show relevant sellers near the buyer.
     *
     * @param lat Buyer's latitude
     * @param lon Buyer's longitude
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<SellerNearbyResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(sellerService.getNearbySellerProfiles(lat, lon));
    }

    @GetMapping("/me")
    public ResponseEntity<SellerNearbyResponse> getCurrentSeller(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(sellerService.getCurrentSellerProfile(userDetails.getUser().getId()));
    }

    @PostMapping(value = "/me/store-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadStoreImages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> urls = sellerService.uploadStoreImages(userDetails.getUser().getId(), files);
        return ResponseEntity.ok(urls);
    }

    @DeleteMapping("/me/store-images")
    public ResponseEntity<Void> deleteStoreImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String imageUrl) {
        sellerService.deleteStoreImage(userDetails.getUser().getId(), imageUrl);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/store-photos/{fileName:.+}")
    public ResponseEntity<Resource> getStorePhoto(@PathVariable String fileName) {
        Resource file = sellerService.loadStorePhoto(fileName);
        MediaType mediaType = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(file);
    }
}
