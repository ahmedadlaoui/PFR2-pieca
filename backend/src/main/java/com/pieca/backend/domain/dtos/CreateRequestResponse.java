package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateRequestResponse {
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private RequestStatus status;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private LocalDateTime createdAt;
}
