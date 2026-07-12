package com.vektor.dispatch_engine.repository;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {
    Optional<DeliveryEvent> findByEventId(UUID eventId);

    Page<DeliveryEvent> findByProcessedFalseAndStatus(DeliveryEventStatus status, Pageable pageable);

    Page<DeliveryEvent> findByProcessedFalseOrderByReceivedAtAsc(Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE DeliveryEvent e SET e.processed = true WHERE e.driverId = :driverId AND e.status = 'DELIVERED' AND e.processed = false AND e.receivedAt  <= :cutoff")
    int markAsProcessedForDriver(@Param("driverId") String driverId, @Param("cutoff") Instant cutoff);
}
