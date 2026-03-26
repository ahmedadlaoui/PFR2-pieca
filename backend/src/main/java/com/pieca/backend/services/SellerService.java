package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.SellerNearbyResponse;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.SellerProfile;
import com.pieca.backend.exceptions.FileProcessingException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.repositories.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private static final Path STORE_PHOTO_DIR = Paths.get("uploads", "stores");
    private static final long MAX_STORE_PHOTO_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_STORE_IMAGES = 6;


    @Transactional
    public List<String> uploadStoreImages(Long userId, List<MultipartFile> files) {
        SellerProfile sp = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil vendeur introuvable"));

        List<String> existing = parseStoreImages(sp.getStoreImagesJson());

        if (files == null || files.isEmpty()) {
            throw new FileProcessingException("Aucun fichier fourni");
        }
        if (existing.size() + files.size() > MAX_STORE_IMAGES) {
            throw new FileProcessingException("Maximum " + MAX_STORE_IMAGES + " images autorisees");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > MAX_STORE_PHOTO_SIZE) {
                throw new FileProcessingException("Chaque image ne doit pas depasser 5 MB");
            }
            String ct = file.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                throw new FileProcessingException("Le fichier doit etre une image valide");
            }

            String ext = ".jpg";
            String orig = file.getOriginalFilename();
            if (orig != null) {
                int idx = orig.lastIndexOf('.');
                if (idx > -1 && idx < orig.length() - 1) ext = orig.substring(idx);
            }

            String fileName = UUID.randomUUID() + ext;
            try {
                Files.createDirectories(STORE_PHOTO_DIR);
                Files.copy(file.getInputStream(), STORE_PHOTO_DIR.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FileProcessingException("Impossible d'enregistrer l'image");
            }
            existing.add("/api/v1/sellers/store-photos/" + fileName);
        }

        sp.setStoreImagesJson(serializeStoreImages(existing));
        sellerProfileRepository.save(sp);
        return existing;
    }

    @Transactional
    public void deleteStoreImage(Long userId, String imageUrl) {
        SellerProfile sp = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil vendeur introuvable"));

        List<String> images = parseStoreImages(sp.getStoreImagesJson());
        images.remove(imageUrl);
        sp.setStoreImagesJson(serializeStoreImages(images));
        sellerProfileRepository.save(sp);
    }

    public Resource loadStorePhoto(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ResourceNotFoundException("Photo introuvable");
        }
        Path filePath = STORE_PHOTO_DIR.resolve(fileName).normalize();
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Photo introuvable");
        }
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new FileProcessingException("Impossible de charger la photo");
        }
    }

    private List<String> parseStoreImages(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            String trimmed = json.trim();
            if (!trimmed.startsWith("[")) return new ArrayList<>();
            trimmed = trimmed.substring(1, trimmed.length() - 1);
            if (trimmed.isBlank()) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            for (String part : trimmed.split(",")) {
                String v = part.trim();
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1);
                }
                if (!v.isBlank()) result.add(v);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse store images JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeStoreImages(List<String> images) {
        if (images == null || images.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < images.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(images.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
