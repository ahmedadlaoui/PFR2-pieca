package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.BuyerRequestItemResponse;
import com.pieca.backend.domain.dtos.CreateRequestRequest;
import com.pieca.backend.domain.dtos.CreateRequestResponse;
import com.pieca.backend.domain.dtos.SellerDashboardStatsResponse;
import com.pieca.backend.domain.dtos.SellerRequestItemResponse;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.Offer;
import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.SellerProfile;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.OfferStatus;
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
import java.math.BigDecimal;
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
    private final com.pieca.backend.repositories.OfferRepository offerRepository;

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

                return requests.map(request -> toBuyerItem(request, latitude, longitude));
        }

        private BuyerRequestItemResponse toBuyerItem(Request request) {
                return toBuyerItem(request, null, null);
        }

        private BuyerRequestItemResponse toBuyerItem(Request request, Double sellerLatitude, Double sellerLongitude) {
                String title = request.getDescription();
                String details = "";
                if (request.getDescription() != null) {
                        int separatorIndex = request.getDescription().indexOf(" | ");
                        if (separatorIndex >= 0) {
                                title = request.getDescription().substring(0, separatorIndex);
                                details = request.getDescription().substring(separatorIndex + 3);
                        }
                }

                Double distanceKm = null;
                if (sellerLatitude != null
                                && sellerLongitude != null
                                && request.getLocation() != null) {
                        distanceKm = haversineKm(
                                        sellerLatitude,
                                        sellerLongitude,
                                        request.getLocation().getY(),
                                        request.getLocation().getX()
                        );
                }

                return BuyerRequestItemResponse.builder()
                                .id(request.getId())
                                .title(title)
                                .description(details)
                                .categoryId(request.getCategory() != null ? request.getCategory().getId() : null)
                                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                                .buyerFirstName(request.getBuyer() != null ? request.getBuyer().getFirstName() : null)
                                .buyerLastName(request.getBuyer() != null ? request.getBuyer().getLastName() : null)
                                .buyerPhone(request.getBuyer() != null ? request.getBuyer().getPhoneNumber() : null)
                                .distanceKm(distanceKm)
                                .status(request.getStatus())
                                .imageUrl(request.getImageUrl())
                                .createdAt(request.getCreatedAt())
                                .build();
        }

        private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
                double earthRadiusKm = 6371.0;
                double dLat = Math.toRadians(lat2 - lat1);
                double dLon = Math.toRadians(lon2 - lon1);
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return earthRadiusKm * c;
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

        @Transactional
        public void acceptRequest(Long requestId, String sellerEmail) {
                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent accepter des demandes");
                }

                Request request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

                if (offerRepository.existsByRequestIdAndSellerId(requestId, seller.getId())) {
                        throw new BusinessViolationException("Vous avez deja accepte cette demande");
                }

                com.pieca.backend.domain.entities.Offer offer = com.pieca.backend.domain.entities.Offer.builder()
                                .price(java.math.BigDecimal.ONE) // Mock price
                                .proofImageUrl("N/A") // Mock image
                                .status(com.pieca.backend.domain.enums.OfferStatus.PENDING)
                                .request(request)
                                .seller(seller)
                                .build();

                offerRepository.save(offer);
        }

        @Transactional(readOnly = true)
        public Page<BuyerRequestItemResponse> getAcceptedRequests(String sellerEmail, int page, int size) {
                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent consulter leurs demandes acceptees");
                }

                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
                Page<com.pieca.backend.domain.entities.Offer> offers = offerRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable);

                SellerProfile profile = sellerProfileRepository.findByUserId(seller.getId()).orElse(null);
                Double latitude = null;
                Double longitude = null;
                
                if (profile != null && profile.getLocation() != null) {
                        latitude = profile.getLocation().getY();
                        longitude = profile.getLocation().getX();
                }
                
                final Double sellerLat = latitude;
                final Double sellerLon = longitude;

                return offers.map(offer -> toBuyerItem(offer.getRequest(), sellerLat, sellerLon));
        }

        @Transactional(readOnly = true)
        public Page<SellerRequestItemResponse> getSellerOffers(String sellerEmail, OfferStatus offerStatus, int page, int size) {
                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent consulter leurs offres");
                }

                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
                Page<Offer> offers = offerStatus == null
                                ? offerRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable)
                                : offerRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(seller.getId(), offerStatus, pageable);

                return offers.map(this::toSellerItem);
        }

        @Transactional(readOnly = true)
        public SellerDashboardStatsResponse getSellerStats(String sellerEmail) {
                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent consulter leurs statistiques");
                }

                Long sellerId = seller.getId();
                long total = offerRepository.countBySellerId(sellerId);
                long pending = offerRepository.countBySellerIdAndStatus(sellerId, OfferStatus.PENDING);
                long accepted = offerRepository.countBySellerIdAndStatus(sellerId, OfferStatus.ACCEPTED);
                long rejected = offerRepository.countBySellerIdAndStatus(sellerId, OfferStatus.REJECTED);
                long cancelled = offerRepository.countBySellerIdAndStatus(sellerId, OfferStatus.CANCELLED);
                BigDecimal revenue = offerRepository.sumPriceBySellerIdAndStatus(sellerId, OfferStatus.ACCEPTED);

                return SellerDashboardStatsResponse.builder()
                                .totalOffers(total)
                                .pendingOffers(pending)
                                .acceptedOffers(accepted)
                                .rejectedOffers(rejected)
                                .cancelledOffers(cancelled)
                                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                                .build();
        }

        @Transactional
        public void cancelOffer(Long requestId, String sellerEmail) {
                User seller = userRepository.findByEmail(sellerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Vendeur introuvable"));

                if (seller.getRole() != Role.SELLER) {
                        throw new UnauthorizedActionException("Seuls les vendeurs peuvent annuler leurs offres");
                }

                Offer offer = offerRepository.findByRequestIdAndSellerId(requestId, seller.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable pour cette demande"));

                if (offer.getStatus() != OfferStatus.PENDING) {
                        throw new BusinessViolationException("Seules les offres en attente peuvent etre annulees");
                }

                offer.setStatus(OfferStatus.CANCELLED);
                offerRepository.save(offer);
        }

        private SellerRequestItemResponse toSellerItem(Offer offer) {
                Request request = offer.getRequest();
                String title = request.getDescription();
                String details = "";
                if (request.getDescription() != null) {
                        int separatorIndex = request.getDescription().indexOf(" | ");
                        if (separatorIndex >= 0) {
                                title = request.getDescription().substring(0, separatorIndex);
                                details = request.getDescription().substring(separatorIndex + 3);
                        }
                }

                return SellerRequestItemResponse.builder()
                                .requestId(request.getId())
                                .offerId(offer.getId())
                                .title(title)
                                .description(details)
                                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                                .requestStatus(request.getStatus())
                                .offerStatus(offer.getStatus())
                                .offerPrice(offer.getPrice())
                                .buyerFirstName(request.getBuyer() != null ? request.getBuyer().getFirstName() : null)
                                .buyerLastName(request.getBuyer() != null ? request.getBuyer().getLastName() : null)
                                .imageUrl(request.getImageUrl())
                                .createdAt(request.getCreatedAt())
                                .offerCreatedAt(offer.getCreatedAt())
                                .build();
        }
}
