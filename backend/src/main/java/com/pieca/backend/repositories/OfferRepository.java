package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.Offer;
import com.pieca.backend.domain.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    Page<Offer> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    Page<Offer> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, OfferStatus status, Pageable pageable);
    boolean existsByRequestIdAndSellerId(Long requestId, Long sellerId);
    Optional<Offer> findByRequestIdAndSellerId(Long requestId, Long sellerId);
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatus(Long sellerId, OfferStatus status);

    @Query("SELECT COALESCE(SUM(o.price), 0) FROM Offer o WHERE o.seller.id = :sellerId AND o.status = :status")
    BigDecimal sumPriceBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") OfferStatus status);
}
