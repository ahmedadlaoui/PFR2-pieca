package com.pieca.backend.domain.dtos;

import lombok.Data;

@Data
public class CategoryDto {
    private Long id;
    private String name;
    private Boolean isActive;
}
