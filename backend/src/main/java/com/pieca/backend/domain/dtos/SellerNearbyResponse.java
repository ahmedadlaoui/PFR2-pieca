package com.pieca.backend.domain.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SellerNearbyResponse {

    private Long sellerProfileId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String sellerType;
    private Integer activeRadiusKm;
    private Double latitude;
    private Double longitude;
    private List<String> categoryNames;
}
