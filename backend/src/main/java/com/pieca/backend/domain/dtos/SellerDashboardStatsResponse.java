package com.pieca.backend.domain.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SellerDashboardStatsResponse {
    private long totalOffers;
    private long pendingOffers;
    private long acceptedOffers;
    private long rejectedOffers;
    private long cancelledOffers;
    private BigDecimal totalRevenue;
    private long totalClients;
    private List<BigDecimal> monthlyRevenue;
}
