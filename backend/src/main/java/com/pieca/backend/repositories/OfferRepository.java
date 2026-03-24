package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    Page<Offer> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    boolean existsByRequestIdAndSellerId(Long requestId, Long sellerId);
}
