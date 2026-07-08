package com.vektor.dispatch_engine.repository;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {
    Page<DeliveryEvent> findByProcessedFalseAndStatus(String status, Pageable pageable);
}
