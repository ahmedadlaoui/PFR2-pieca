package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.SellerNearbyResponse;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.SellerProfile;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.repositories.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;

    /**
     * Returns all sellers whose coverage circle contains the given buyer location.
     * Delegates the spatial query to PostGIS via ST_DWithin.
     */
    @Transactional(readOnly = true)
    public List<SellerNearbyResponse> getNearbySellerProfiles(double lat, double lon) {
        log.info("Finding sellers near lat={}, lon={}", lat, lon);
        List<SellerProfile> profiles = sellerProfileRepository.findSellersWithinRadius(lat, lon);
        log.info("Found {} sellers near the requested location", profiles.size());
        return profiles.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SellerNearbyResponse getCurrentSellerProfile(Long userId) {
        SellerProfile sp = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil vendeur introuvable"));
        return toResponse(sp);
    }

    private SellerNearbyResponse toResponse(SellerProfile sp) {
        Double lat = null;
        Double lon = null;
        if (sp.getLocation() != null) {
            lat = sp.getLocation().getY(); // latitude = Y
            lon = sp.getLocation().getX(); // longitude = X
        }

        List<String> categoryNames = sp.getCategories() == null
                ? List.of()
                : sp.getCategories().stream().map(Category::getName).toList();

        return SellerNearbyResponse.builder()
                .sellerProfileId(sp.getId())
                .userId(sp.getUser().getId())
                .firstName(sp.getUser().getFirstName())
                .lastName(sp.getUser().getLastName())
                .profileImageUrl(sp.getUser().getProfileImageUrl())
                .sellerType(sp.getSellerType().name())
                .activeRadiusKm(sp.getActiveRadiusKm())
                .latitude(lat)
                .longitude(lon)
                .categoryNames(categoryNames)
                .build();
    }
}
