package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerOfferItemResponse {
    private Long offerId;
    private BigDecimal price;
    private String proofImageUrl;
    private OfferStatus offerStatus;
    private LocalDateTime offerCreatedAt;

    private Long requestId;
    private String requestTitle;
    private String requestDescription;
    private String requestCategoryName;
    private String requestImageUrl;
    private String requestStatus;

    private String sellerName;
    private String sellerEmail;
    private String sellerPhone;
    private String storeName;
    private String sellerType;
    private List<String> sellerCategories;
    private Double sellerLatitude;
    private Double sellerLongitude;
    private Integer sellerActiveRadiusKm;
    private List<String> sellerStoreImages;
}
