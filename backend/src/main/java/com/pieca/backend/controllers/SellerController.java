package com.pieca.backend.controllers;

import com.pieca.backend.domain.dtos.SellerNearbyResponse;
import com.pieca.backend.services.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
