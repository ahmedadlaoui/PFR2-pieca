package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BuyerRequestItemResponse {
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String buyerFirstName;
    private String buyerLastName;
    private String buyerPhone;
    private Double distanceKm;
    private RequestStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;
}
