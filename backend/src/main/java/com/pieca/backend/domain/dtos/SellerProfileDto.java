package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.SellerType;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Data
public class SellerProfileDto {
    private Long id;
    private SellerType sellerType;
    private Point location;
    private Integer activeRadiusKm;
    private String customCategoryNote;
    private LocalDateTime createdAt;
}
