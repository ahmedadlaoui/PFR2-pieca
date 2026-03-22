package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String phoneNumber;
    private Role role;
    private LocalDateTime createdAt;
}
