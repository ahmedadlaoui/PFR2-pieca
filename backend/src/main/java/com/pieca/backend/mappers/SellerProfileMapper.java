package com.pieca.backend.mappers;

import com.pieca.backend.domain.dtos.SellerProfileDto;
import com.pieca.backend.domain.entities.SellerProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SellerProfileMapper {
    SellerProfileDto toDto(SellerProfile sellerProfile);
    SellerProfile toEntity(SellerProfileDto sellerProfileDto);
}
