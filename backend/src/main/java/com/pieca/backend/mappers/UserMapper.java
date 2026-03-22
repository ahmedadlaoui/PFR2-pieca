package com.pieca.backend.mappers;

import com.pieca.backend.domain.dtos.UserDto;
import com.pieca.backend.domain.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
