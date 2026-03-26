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
@lombok.extern.slf4j.Slf4j
public class RequestService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
        private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024L * 1024L;
        private static final Path REQUEST_PHOTO_DIR = Paths.get("uploads", "requests");

    private final RequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final com.pieca.backend.repositories.OfferRepository offerRepository;
    private final EmailService emailService;

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
                User sellerUser = offer.getSeller();
                String sName = (sellerUser.getFirstName() != null ? sellerUser.getFirstName() : "") + " " + 
                               (sellerUser.getLastName() != null ? sellerUser.getLastName() : "");

                SellerProfile sp = sellerProfileRepository.findByUserId(sellerUser.getId()).orElse(null);

                String sellerType = null;
                java.util.List<String> sellerCategories = new java.util.ArrayList<>();
                Double sellerLat = null;
                Double sellerLon = null;
                Integer sellerRadius = null;
                java.util.List<String> storeImages = new java.util.ArrayList<>();

                if (sp != null) {
                    sellerType = sp.getSellerType() != null ? sp.getSellerType().name() : null;
                    if (sp.getCategories() != null) {
                        for (Category cat : sp.getCategories()) {
                            sellerCategories.add(cat.getName());
                        }
                    }
                    if (sp.getLocation() != null) {
                        sellerLat = sp.getLocation().getY();
                        sellerLon = sp.getLocation().getX();
                    }
                    sellerRadius = sp.getActiveRadiusKm();
                    if (sp.getStoreImagesJson() != null && !sp.getStoreImagesJson().isBlank()) {
                        try {
                            storeImages = parseStoreImagesJson(sp.getStoreImagesJson());
                        } catch (Exception e) {
                            log.warn("Failed to parse store images JSON for seller {}: {}", sellerUser.getId(), e.getMessage());
                        }
                    }
                }

                offerDtos.add(com.pieca.backend.domain.dtos.BuyerRequestDetailsResponse.OfferDto.builder()
                        .id(offer.getId())
                        .price(offer.getPrice())
                        .proofImageUrl(offer.getProofImageUrl())
                        .status(offer.getStatus())
                        .createdAt(offer.getCreatedAt())
                        .sellerName(sName.trim())
                        .sellerEmail(sellerUser.getEmail())
                        .sellerPhone(sellerUser.getPhoneNumber())
                        .storeName(sName.trim())
                        .sellerType(sellerType)
                        .sellerCategories(sellerCategories)
                        .sellerLatitude(sellerLat)
                        .sellerLongitude(sellerLon)
                        .sellerActiveRadiusKm(sellerRadius)
                        .sellerStoreImages(storeImages)
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

        private java.util.List<String> parseStoreImagesJson(String json) {
                if (json == null || json.isBlank()) return new java.util.ArrayList<>();
                String trimmed = json.trim();
                if (!trimmed.startsWith("[")) return new java.util.ArrayList<>();
                trimmed = trimmed.substring(1, trimmed.length() - 1);
                if (trimmed.isBlank()) return new java.util.ArrayList<>();
                java.util.List<String> result = new java.util.ArrayList<>();
                for (String part : trimmed.split(",")) {
                        String v = part.trim();
                        if (v.startsWith("\"") && v.endsWith("\"")) {
                                v = v.substring(1, v.length() - 1);
                        }
                        if (!v.isBlank()) result.add(v);
                }
                return result;
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
        public void acceptRequest(Long requestId, String sellerEmail, BigDecimal price) {
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

                BigDecimal offerPrice = (price != null && price.compareTo(BigDecimal.ZERO) > 0) ? price : BigDecimal.ONE;

                com.pieca.backend.domain.entities.Offer offer = com.pieca.backend.domain.entities.Offer.builder()
                                .price(offerPrice)
                                .proofImageUrl("N/A")
                                .status(com.pieca.backend.domain.enums.OfferStatus.PENDING)
                                .request(request)
                                .seller(seller)
                                .build();

                offerRepository.save(offer);

                try {
                        emailService.sendOfferNotification(request, seller, offerPrice);
                } catch (Exception e) {
                        log.warn("Failed to send email notification for request {}: {}", requestId, e.getMessage());
                }
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
                long clients = offerRepository.countDistinctBuyersBySellerIdAndStatus(sellerId, OfferStatus.ACCEPTED);

                int currentYear = java.time.LocalDate.now().getYear();
                List<Object[]> monthlyData = offerRepository.sumMonthlyRevenueBySellerIdAndStatusAndYear(sellerId, OfferStatus.ACCEPTED, currentYear);
                java.util.List<BigDecimal> monthlyRevenue = new java.util.ArrayList<>(java.util.Collections.nCopies(12, BigDecimal.ZERO));
                for (Object[] row : monthlyData) {
                        int month = ((Number) row[0]).intValue();
                        BigDecimal amount = (BigDecimal) row[1];
                        if (month >= 1 && month <= 12) {
                                monthlyRevenue.set(month - 1, amount);
                        }
                }

                return SellerDashboardStatsResponse.builder()
                                .totalOffers(total)
                                .pendingOffers(pending)
                                .acceptedOffers(accepted)
                                .rejectedOffers(rejected)
                                .cancelledOffers(cancelled)
                                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                                .totalClients(clients)
                                .monthlyRevenue(monthlyRevenue)
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

        @Transactional
        public void buyerAcceptOffer(Long offerId, String buyerEmail) {
                User buyer = userRepository.findByEmail(buyerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));

                if (buyer.getRole() != Role.BUYER) {
                        throw new UnauthorizedActionException("Seuls les acheteurs peuvent accepter des offres");
                }

                Offer offer = offerRepository.findById(offerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));

                if (!offer.getRequest().getBuyer().getId().equals(buyer.getId())) {
                        throw new UnauthorizedActionException("Vous n'etes pas autorise a modifier cette offre");
                }

                if (offer.getStatus() != OfferStatus.PENDING) {
                        throw new BusinessViolationException("Seules les offres en attente peuvent etre acceptees");
                }

                offer.setStatus(OfferStatus.ACCEPTED);
                offerRepository.save(offer);

                Request request = offer.getRequest();
                request.setStatus(RequestStatus.RESOLVED);
                requestRepository.save(request);

                try {
                        emailService.sendOfferAcceptedNotification(offer.getRequest(), offer.getSeller(), buyer, offer.getPrice());
                } catch (Exception e) {
                        log.warn("Failed to send offer accepted email for offer {}: {}", offerId, e.getMessage());
                }
        }

        @Transactional
        public void buyerDeclineOffer(Long offerId, String buyerEmail) {
                User buyer = userRepository.findByEmail(buyerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));

                if (buyer.getRole() != Role.BUYER) {
                        throw new UnauthorizedActionException("Seuls les acheteurs peuvent rejeter des offres");
                }

                Offer offer = offerRepository.findById(offerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));

                if (!offer.getRequest().getBuyer().getId().equals(buyer.getId())) {
                        throw new UnauthorizedActionException("Vous n'etes pas autorise a modifier cette offre");
                }

                if (offer.getStatus() != OfferStatus.PENDING) {
                        throw new BusinessViolationException("Seules les offres en attente peuvent etre rejetees");
                }

                offer.setStatus(OfferStatus.REJECTED);
                offerRepository.save(offer);

                try {
                        emailService.sendOfferDeclinedNotification(offer.getRequest(), offer.getSeller(), buyer);
                } catch (Exception e) {
                        log.warn("Failed to send offer declined email for offer {}: {}", offerId, e.getMessage());
                }
        }

        @Transactional(readOnly = true)
        public Page<com.pieca.backend.domain.dtos.BuyerOfferItemResponse> getBuyerOffers(
                        String buyerEmail, OfferStatus status, String period, int page, int size) {
                User buyer = userRepository.findByEmail(buyerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Acheteur introuvable"));
                if (buyer.getRole() != Role.BUYER) {
                        throw new UnauthorizedActionException("Seuls les acheteurs peuvent consulter leurs offres");
                }

                java.time.LocalDateTime since = null;
                if ("24h".equals(period)) {
                        since = java.time.LocalDateTime.now().minusHours(24);
                } else if ("week".equals(period)) {
                        since = java.time.LocalDateTime.now().minusWeeks(1);
                }

                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
                Page<Offer> offers;
                if (status != null && since != null) {
                        offers = offerRepository.findByBuyerIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(buyer.getId(), status, since, pageable);
                } else if (status != null) {
                        offers = offerRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyer.getId(), status, pageable);
                } else if (since != null) {
                        offers = offerRepository.findByBuyerIdAndCreatedAtAfterOrderByCreatedAtDesc(buyer.getId(), since, pageable);
                } else {
                        offers = offerRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable);
                }

                return offers.map(this::toBuyerOfferItem);
        }

        private com.pieca.backend.domain.dtos.BuyerOfferItemResponse toBuyerOfferItem(Offer offer) {
                Request request = offer.getRequest();
                String title = request.getDescription();
                String details = "";
                if (request.getDescription() != null) {
                        int sep = request.getDescription().indexOf(" | ");
                        if (sep >= 0) {
                                title = request.getDescription().substring(0, sep);
                                details = request.getDescription().substring(sep + 3);
                        }
                }

                User sellerUser = offer.getSeller();
                String sellerName = ((sellerUser.getFirstName() != null ? sellerUser.getFirstName() : "") + " " +
                                (sellerUser.getLastName() != null ? sellerUser.getLastName() : "")).trim();

                String storeName = null;
                String sellerTypeStr = null;
                java.util.List<String> sellerCategories = new java.util.ArrayList<>();
                Double sellerLat = null;
                Double sellerLon = null;
                Integer sellerRadius = null;
                java.util.List<String> storeImages = new java.util.ArrayList<>();

                SellerProfile sp = sellerProfileRepository.findByUserId(sellerUser.getId()).orElse(null);
                if (sp != null) {
                        storeName = sp.getCustomCategoryNote();
                        if (sp.getSellerType() != null) sellerTypeStr = sp.getSellerType().name();
                        if (sp.getCategories() != null) {
                                for (Category cat : sp.getCategories()) {
                                        sellerCategories.add(cat.getName());
                                }
                        }
                        if (sp.getLocation() != null) {
                                sellerLat = sp.getLocation().getY();
                                sellerLon = sp.getLocation().getX();
                        }
                        sellerRadius = sp.getActiveRadiusKm();
                        if (sp.getStoreImagesJson() != null && !sp.getStoreImagesJson().isBlank()) {
                                try { storeImages = parseStoreImagesJson(sp.getStoreImagesJson()); } catch (Exception ignored) {}
                        }
                }

                return com.pieca.backend.domain.dtos.BuyerOfferItemResponse.builder()
                                .offerId(offer.getId())
                                .price(offer.getPrice())
                                .proofImageUrl(offer.getProofImageUrl())
                                .offerStatus(offer.getStatus())
                                .offerCreatedAt(offer.getCreatedAt())
                                .requestId(request.getId())
                                .requestTitle(title)
                                .requestDescription(details)
                                .requestCategoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                                .requestImageUrl(request.getImageUrl())
                                .requestStatus(request.getStatus() != null ? request.getStatus().name() : null)
                                .sellerName(sellerName)
                                .sellerEmail(sellerUser.getEmail())
                                .sellerPhone(sellerUser.getPhoneNumber())
                                .storeName(storeName)
                                .sellerType(sellerTypeStr)
                                .sellerCategories(sellerCategories)
                                .sellerLatitude(sellerLat)
                                .sellerLongitude(sellerLon)
                                .sellerActiveRadiusKm(sellerRadius)
                                .sellerStoreImages(storeImages)
                                .build();
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
                                .buyerPhone(request.getBuyer() != null ? request.getBuyer().getPhoneNumber() : null)
                                .buyerEmail(request.getBuyer() != null ? request.getBuyer().getEmail() : null)
                                .imageUrl(request.getImageUrl())
                                .createdAt(request.getCreatedAt())
                                .offerCreatedAt(offer.getCreatedAt())
                                .build();
        }
}
