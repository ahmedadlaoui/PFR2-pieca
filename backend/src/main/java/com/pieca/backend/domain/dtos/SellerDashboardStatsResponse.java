package com.pieca.backend.domain.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SellerDashboardStatsResponse {
    private long totalOffers;
    private long pendingOffers;
    private long acceptedOffers;
    private long rejectedOffers;
    private long cancelledOffers;
    private BigDecimal totalRevenue;
}
