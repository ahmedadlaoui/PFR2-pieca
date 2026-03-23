package com.pieca.backend.services;

import com.pieca.backend.domain.dtos.CreateRequestRequest;
import com.pieca.backend.domain.dtos.CreateRequestResponse;
import com.pieca.backend.domain.entities.Category;
import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.entities.User;
import com.pieca.backend.domain.enums.Role;
import com.pieca.backend.exceptions.BusinessViolationException;
import com.pieca.backend.exceptions.ResourceNotFoundException;
import com.pieca.backend.exceptions.UnauthorizedActionException;
import com.pieca.backend.repositories.CategoryRepository;
import com.pieca.backend.repositories.RequestRepository;
import com.pieca.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final RequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateRequestResponse createBuyerRequest(CreateRequestRequest payload, String buyerEmail) {
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

        Request request = Request.builder()
                .description(persistedDescription)
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
                .radiusKm(payload.getRadiusKm())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
