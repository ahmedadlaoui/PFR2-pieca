package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerRequestDetailsResponse {
    private Long id;
    private String title;
    private String description;
    private String categoryName;
    private RequestStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;
    private List<OfferDto> offers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferDto {
        private Long id;
        private java.math.BigDecimal price;
        private String proofImageUrl;
        private com.pieca.backend.domain.enums.OfferStatus status;
        private LocalDateTime createdAt;
        private String sellerName;
        private String sellerEmail;
        private String sellerPhone;
        private String storeName;
    }
}
