package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.BuyerRequestItemResponse;
import com.pieca.backend.domain.dtos.CreateRequestRequest;
import com.pieca.backend.domain.dtos.CreateRequestResponse;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.SellerProfile;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.RequestStatus;
import com.pieca.backend.domain.enums.Role;
import com.pieca.backend.exceptions.BusinessViolationException;
import com.pieca.backend.exceptions.FileProcessingException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.exceptions.UnauthorizedActionException;
import com.pieca.backend.repositories.CategoryRepository;
import com.pieca.backend.repositories.RequestRepository;
import com.pieca.backend.repositories.SellerProfileRepository;
import com.pieca.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
        private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024L * 1024L;
        private static final Path REQUEST_PHOTO_DIR = Paths.get("uploads", "requests");

    private final RequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional
    public CreateRequestResponse createBuyerRequest(CreateRequestRequest payload, String buyerEmail, MultipartFile photo) {
                if (buyerEmail == null || buyerEmail.isBlank()) {
                        throw new UnauthorizedActionException("Authentification requise");
                }

        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));

        if (buyer.getRole() != Role.BUYER) {
            throw new UnauthorizedActionException("Seuls les acheteurs peuvent creer une demande");
        }

        Category category = categoryRepository.findById(payload.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categorie introuvable"));

                if (!Boolean.TRUE.equals(category.getIsActive())) {
                        throw new BusinessViolationException("Cette categorie est desactivee");
                }

        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(payload.getLongitude(), payload.getLatitude()));
        location.setSRID(4326);

                String normalizedTitle = payload.getTitle() == null ? "" : payload.getTitle().trim();
                if (normalizedTitle.length() < 3) {
                        throw new BusinessViolationException("Le titre doit contenir au moins 3 caracteres");
                }

        String normalizedDetails = payload.getDescription() == null ? "" : payload.getDescription().trim();
        String persistedDescription = normalizedDetails.isBlank()
                ? normalizedTitle
                : normalizedTitle + " | " + normalizedDetails;

        String imageUrl = storePhoto(photo);

        Request request = Request.builder()
                .description(persistedDescription)
                .imageUrl(imageUrl)
                .location(location)
                .category(category)
                .buyer(buyer)
                .build();

        Request saved = requestRepository.save(request);

        return CreateRequestResponse.builder()
                .id(saved.getId())
                .title(normalizedTitle)
                .description(normalizedDetails)
                .categoryId(category.getId())
                .status(saved.getStatus())
                .latitude(saved.getLocation() != null ? saved.getLocation().getY() : null)
                .longitude(saved.getLocation() != null ? saved.getLocation().getX() : null)
                                .imageUrl(saved.getImageUrl())
                .radiusKm(payload.getRadiusKm())
                .createdAt(saved.getCreatedAt())
                .build();
    }

        @Transactional(readOnly = true)
        public Page<BuyerRequestItemResponse> getBuyerRequests(String buyerEmail, RequestStatus status, int page, int size) {
                if (buyerEmail == null || buyerEmail.isBlank()) {
                        throw new UnauthorizedActionException("Authentification requise");
                }

                User buyer = userRepository.findByEmail(buyerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));

                if (buyer.getRole() != Role.BUYER) {
                        throw new UnauthorizedActionException("Seuls les acheteurs peuvent consulter leurs demandes");
                }

                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

                Page<Request> requests = status == null
                                ? requestRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable)
                                : requestRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyer.getId(), status, pageable);

                return requests.map(this::toBuyerItem);
        }

            @Transactional(readOnly = true)
    public com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse getBuyerRequestDetails(Long requestId, String buyerEmail) {
        if (buyerEmail == null || buyerEmail.isBlank()) {
            throw new UnauthorizedActionException("Authentification requise");
        }

        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (!request.getBuyer().getId().equals(buyer.getId())) {
            throw new UnauthorizedActionException("Vous n'etes pas autorise a voir cette demande");
        }

        String rawDesc = request.getDescription() == null ? "" : request.getDescription();
        int separatorIdx = rawDesc.indexOf(" | ");
        String baseTitle = rawDesc;
        String baseDesc = "";
        
        if (separatorIdx != -1) {
            baseTitle = rawDesc.substring(0, separatorIdx);
            baseDesc = rawDesc.substring(separatorIdx + 3);
        } else if (rawDesc.length() > 0) {
            baseTitle = rawDesc;
        }

        java.util.List<com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse.OfferDto> offerDtos = new java.util.ArrayList<>();
        if (request.getOffers() != null) {
            for (com.pieca.backend.domain.entities.Offer offer : request.getOffers()) {
                String sName = (offer.getSeller().getFirstName() != null ? offer.getSeller().getFirstName() : "") + " " + 
                               (offer.getSeller().getLastName() != null ? offer.getSeller().getLastName() : "");
                String storeName = null;

                offerDtos.add(com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse.OfferDto.builder()
                        .id(offer.getId())
                        .price(offer.getPrice())
                        .proofImageUrl(offer.getProofImageUrl())
                        .status(offer.getStatus())
                        .createdAt(offer.getCreatedAt())
                        .sellerName(sName.trim())
                        .sellerEmail(offer.getSeller().getEmail())
                        .sellerPhone(offer.getSeller().getPhoneNumber())
                        .storeName(storeName)
                        .build());
            }
        }

        return com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse.builder()
                .id(request.getId())
                .title(baseTitle)
                .description(baseDesc)
                .categoryName(request.getCategory().getName())
                .status(request.getStatus())
                .imageUrl(request.getImageUrl())
                .createdAt(request.getCreatedAt())
                .offers(offerDtos)
                .build();
    }

	public Resource loadRequestPhoto(String fileName) {
                if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                        throw new ResourceNotFoundException("Photo introuvable");
                }

                Path filePath = REQUEST_PHOTO_DIR.resolve(fileName).normalize();

                if (!Files.exists(filePath)) {
                        throw new ResourceNotFoundException("Photo introuvable");
                }

                try {
                        return new UrlResource(filePath.toUri());
                } catch (MalformedURLException e) {
                        throw new FileProcessingException("Impossible de charger la photo demandee");
                }
        }

        @Transactional(readOnly = true)
        public Page<BuyerRequestItemResponse> getNearbyRequests(String sellerEmail, int page, int size) {
                if (sellerEmail == null || sellerEmail.isBlank()) {
                        throw new UnauthorizedActionException("Authentification requise");
                }

                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent consulter les demandes a proximite");
                }

                SellerProfile profile = sellerProfileRepository.findByUserId(seller.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Profil vendeur introuvable"));

                if (profile.getLocation() == null || profile.getCategories() == null || profile.getCategories().isEmpty()) {
                        return Page.empty(PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
                }

                double latitude = profile.getLocation().getY();
                double longitude = profile.getLocation().getX();
                double radiusKm = profile.getActiveRadiusKm() != null ? profile.getActiveRadiusKm() : 5.0;

                List<Long> categoryIds = profile.getCategories().stream()
                                .map(Category::getId)
                                .collect(Collectors.toList());

                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
                
                Page<Request> requests = requestRepository.findNearbyMatchingRequests(
                                longitude, latitude, radiusKm, categoryIds, pageable
                );

                return requests.map(this::toBuyerItem);
        }

        private BuyerRequestItemResponse toBuyerItem(Request request) {
                String title = request.getDescription();
                String details = "";
                if (request.getDescription() != null) {
                        int separatorIndex = request.getDescription().indexOf(" | ");
                        if (separatorIndex >= 0) {
                                title = request.getDescription().substring(0, separatorIndex);
                                details = request.getDescription().substring(separatorIndex + 3);
                        }
                }

                return BuyerRequestItemResponse.builder()
                                .id(request.getId())
                                .title(title)
                                .description(details)
                                .categoryId(request.getCategory() != null ? request.getCategory().getId() : null)
                                .status(request.getStatus())
                                .imageUrl(request.getImageUrl())
                                .createdAt(request.getCreatedAt())
                                .build();
        }

        private String storePhoto(MultipartFile photo) {
                if (photo == null || photo.isEmpty()) {
                        return null;
                }

                if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
                        throw new FileProcessingException("La photo ne doit pas depasser 5 MB");
                }

                String contentType = photo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                        throw new FileProcessingException("Le fichier doit etre une image valide");
                }

                String extension = ".jpg";
                String originalName = photo.getOriginalFilename();
                if (originalName != null) {
                        int index = originalName.lastIndexOf('.');
                        if (index > -1 && index < originalName.length() - 1) {
                                extension = originalName.substring(index);
                        }
                }

                String fileName = UUID.randomUUID() + extension;

                try {
                        Files.createDirectories(REQUEST_PHOTO_DIR);
                        Path destination = REQUEST_PHOTO_DIR.resolve(fileName);
                        Files.copy(photo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                        throw new FileProcessingException("Impossible d'enregistrer la photo de la demande");
                }

                return "/api/v1/requests/photos/" + fileName;
        }
}

