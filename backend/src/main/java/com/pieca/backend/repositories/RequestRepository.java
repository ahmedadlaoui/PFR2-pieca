package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
	Page<Request> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

	Page<Request> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, RequestStatus status, Pageable pageable);
}
