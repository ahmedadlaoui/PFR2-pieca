package com.pieca.backend.mappers;

import com.pieca.backend.domain.dtos.RequestDto;
import com.pieca.backend.domain.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "buyer.id", target = "buyerId")
    RequestDto toDto(Request request);

    @Mapping(source = "categoryId", target = "category.id")
    @Mapping(source = "buyerId", target = "buyer.id")
    Request toEntity(RequestDto requestDto);
}
