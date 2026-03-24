package com.pieca.backend.repositories;

import com.pieca.backend.domain.entities.Request;
import com.pieca.backend.domain.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
	Page<Request> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

	Page<Request> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, RequestStatus status, Pageable pageable);

	@Query(value = "SELECT r.* FROM requests r " +
			       "WHERE ST_DWithin(r.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusKm * 1000) " +
			       "AND r.category_id IN (:categoryIds) " +
			       "AND r.status = 'PENDING' " +
			       "ORDER BY ST_Distance(r.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)", 
	       nativeQuery = true)
	Page<Request> findNearbyMatchingRequests(@Param("longitude") double longitude, 
	                                         @Param("latitude") double latitude, 
	                                         @Param("radiusKm") double radiusKm, 
	                                         @Param("categoryIds") List<Long> categoryIds, 
	                                         Pageable pageable);
}
