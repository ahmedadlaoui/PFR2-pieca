package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByUserId(Long userId);

    /**
     * Finds all sellers whose service circle covers the given buyer location.
     * Uses PostGIS ST_DWithin on geography type for accurate km-to-meter conversion.
     */
    @Query(value = """
        SELECT sp.* FROM seller_profiles sp
        WHERE sp.location IS NOT NULL
          AND ST_DWithin(
            sp.location::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
            sp.active_radius_km * 1000
          )
        """, nativeQuery = true)
    List<SellerProfile> findSellersWithinRadius(@Param("lat") double lat, @Param("lon") double lon);
}
