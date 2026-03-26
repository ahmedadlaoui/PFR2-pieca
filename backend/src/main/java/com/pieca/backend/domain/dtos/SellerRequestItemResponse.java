package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.OfferStatus;
import com.pieca.backend.domain.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SellerRequestItemResponse {
    private Long requestId;
    private Long offerId;
    private String title;
    private String description;
    private String categoryName;
    private RequestStatus requestStatus;
    private OfferStatus offerStatus;
    private BigDecimal offerPrice;
    private String buyerFirstName;
    private String buyerLastName;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime offerCreatedAt;
}
