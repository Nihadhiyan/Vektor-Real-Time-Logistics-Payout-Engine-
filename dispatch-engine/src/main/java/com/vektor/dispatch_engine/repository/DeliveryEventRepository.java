package com.vektor.dispatch_engine.repository;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {
    Optional<DeliveryEvent> findByEventId(UUID eventId);

    Page<DeliveryEvent> findByProcessedFalseAndStatus(DeliveryEventStatus status, Pageable pageable);

    Page<DeliveryEvent> findByProcessedFalseOrderByReceivedAtAsc(Pageable pageable);
}
