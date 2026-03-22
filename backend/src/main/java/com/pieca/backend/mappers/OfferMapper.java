package com.pieca.backend.mappers;

import com.pieca.backend.domain.dtos.OfferDto;
import com.pieca.backend.domain.entities.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OfferMapper {
    @Mapping(source = "request.id", target = "requestId")
    @Mapping(source = "seller.id", target = "sellerId")
    OfferDto toDto(Offer offer);

    @Mapping(source = "requestId", target = "request.id")
    @Mapping(source = "sellerId", target = "seller.id")
    Offer toEntity(OfferDto offerDto);
}
