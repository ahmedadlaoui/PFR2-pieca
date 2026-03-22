package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.RequestStatus;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Data
public class RequestDto {
    private Long id;
    private String description;
    private String imageUrl;
    private Point location;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long categoryId;
    private Long buyerId;
}
