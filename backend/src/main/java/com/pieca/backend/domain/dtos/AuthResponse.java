package com.pieca.backend.domain.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pieca.backend.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    
    @JsonIgnore
    private String refreshToken;
    
    private Role role;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
}
