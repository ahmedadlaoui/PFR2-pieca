package com.pieca.backend.domain.entities;

import com.pieca.backend.domain.enums.SellerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerType sellerType;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer activeRadiusKm = 5;

    private String customCategoryNote;

    @Column(columnDefinition = "TEXT")
    private String storeImagesJson;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "seller_profile_categories",
        joinColumns = @JoinColumn(name = "seller_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;
}
