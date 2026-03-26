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
import java.time.LocalDateTime;
import java.util.List;
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

    @Query("SELECT EXTRACT(MONTH FROM o.createdAt) AS month, COALESCE(SUM(o.price), 0) AS total FROM Offer o WHERE o.seller.id = :sellerId AND o.status = :status AND EXTRACT(YEAR FROM o.createdAt) = :year GROUP BY EXTRACT(MONTH FROM o.createdAt) ORDER BY month")
    List<Object[]> sumMonthlyRevenueBySellerIdAndStatusAndYear(@Param("sellerId") Long sellerId, @Param("status") OfferStatus status, @Param("year") int year);

    @Query("SELECT COUNT(DISTINCT o.request.buyer.id) FROM Offer o WHERE o.seller.id = :sellerId AND o.status = :status")
    long countDistinctBuyersBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") OfferStatus status);

    Optional<Offer> findById(Long id);

    @Query("SELECT o FROM Offer o WHERE o.request.buyer.id = :buyerId ORDER BY o.createdAt DESC")
    Page<Offer> findByBuyerIdOrderByCreatedAtDesc(@Param("buyerId") Long buyerId, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.request.buyer.id = :buyerId AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    Page<Offer> findByBuyerIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("buyerId") Long buyerId, @Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.request.buyer.id = :buyerId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Offer> findByBuyerIdAndStatusOrderByCreatedAtDesc(@Param("buyerId") Long buyerId, @Param("status") OfferStatus status, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.request.buyer.id = :buyerId AND o.status = :status AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    Page<Offer> findByBuyerIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(@Param("buyerId") Long buyerId, @Param("status") OfferStatus status, @Param("since") LocalDateTime since, Pageable pageable);
}
