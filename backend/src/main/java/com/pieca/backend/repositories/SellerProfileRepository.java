package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
}
