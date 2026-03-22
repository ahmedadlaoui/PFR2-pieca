package com.pieca.backend.domain.dtos;

import com.pieca.backend.domain.enums.SellerType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RegisterSellerRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotNull(message = "Le type de vendeur est obligatoire")
    private SellerType sellerType;

    @NotEmpty(message = "Au moins une catégorie est requise")
    private List<Long> categoryIds;

    private Double latitude;

    private Double longitude;

    private Integer activeRadiusKm;

    private String customCategoryNote;
}
