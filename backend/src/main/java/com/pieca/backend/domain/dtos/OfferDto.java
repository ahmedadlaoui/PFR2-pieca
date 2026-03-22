package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.OfferStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OfferDto {
    private Long id;
    private BigDecimal price;
    private String proofImageUrl;
    private OfferStatus status;
    private LocalDateTime createdAt;
    private Long requestId;
    private Long sellerId;
}
