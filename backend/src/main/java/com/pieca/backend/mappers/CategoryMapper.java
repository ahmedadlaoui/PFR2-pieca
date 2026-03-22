package com.pieca.backend.mappers;

import com.pieca.backend.domain.dtos.CategoryDto;
import com.pieca.backend.domain.entities.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);
    Category toEntity(CategoryDto categoryDto);
}
