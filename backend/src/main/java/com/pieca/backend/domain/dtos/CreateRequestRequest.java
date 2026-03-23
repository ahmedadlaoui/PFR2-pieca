package com.pieca.backend.domain.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRequestRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 180, message = "Le titre doit contenir entre 3 et 180 caracteres")
    private String title;

    @Size(max = 1000, message = "La description ne doit pas depasser 1000 caracteres")
    private String description;

    @NotNull(message = "La categorie est obligatoire")
    private Long categoryId;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitude;

    @NotNull(message = "Le rayon est obligatoire")
    @DecimalMin(value = "1.0", message = "Le rayon minimum est de 1 km")
    @DecimalMax(value = "100.0", message = "Le rayon maximum est de 100 km")
    private Double radiusKm;
}
