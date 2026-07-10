package com.vektor.dispatch_engine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vektor.dispatch_engine.model.PayoutOutbox;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;

@Repository
public interface PayoutOutboxRepository extends JpaRepository<PayoutOutbox, UUID> {

    List<PayoutOutbox> findByStatus(DriverPayoutStatus status);
}
