package com.vektor.dispatch_engine.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vektor.dispatch_engine.model.PayoutOutbox;

@Repository
public interface PayoutOutboxRepository extends JpaRepository<PayoutOutbox, UUID> {

    @Query("SELECT o.outboxId FROM PayoutOutbox o WHERE o.status = 'PENDING'")
    List<UUID> findPendingIds();

    // Crash Recovery: Finds records that have been stuck in PROCESSING for longer than the threshold
    @Query("SELECT p.outboxId FROM PayoutOutbox p WHERE p.status = 'PROCESSING' AND p.claimedAt < :threshold")
    List<UUID> findStuckProcessingIds(@Param("threshold") Instant threshold);

    @Modifying
    @Transactional
    @Query("UPDATE PayoutOutbox p SET p.status = 'PROCESSING', p.claimedAt = :now WHERE p.outboxId = :id AND (p.status = 'PENDING' OR (p.status = 'PROCESSING' AND p.claimedAt < :stuckThreshold))")
    int claim(@Param("id") UUID id,
              @Param("now") Instant now,
              @Param("stuckThreshold") Instant stuckThreshold);
}
