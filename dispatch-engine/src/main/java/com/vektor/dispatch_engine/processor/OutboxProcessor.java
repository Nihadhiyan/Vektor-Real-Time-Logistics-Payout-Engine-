package com.vektor.dispatch_engine.processor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.vektor.dispatch_engine.event.PayoutOutboxCreatedEvent;
import com.vektor.dispatch_engine.repository.PayoutOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final PayoutOutboxRepository outboxRepository;
    private final OutboxRecordProcessor recordProcessor;

    @Value("${vektor.outbox.stuck-threshold-minutes:5}")
    private long stuckThresholdMinutes;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutboxEvent(PayoutOutboxCreatedEvent event) {
        log.debug("Outbox wake-up: processing pending records");
        processPending();
    }

    @Scheduled(fixedDelayString = "${vektor.outbox.sweep-ms:60000}")
    public void scheduledSweep() {
        processPending();
        recoverStuck();
    }

    private void processPending() {
        for (UUID id : outboxRepository.findPendingIds()) {
            safeProcess(id);
        }
    }

    private void recoverStuck() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(stuckThresholdMinutes));
        for (UUID id : outboxRepository.findStuckProcessingIds(threshold)) {
            log.warn("Recovering outbox record stuck in PROCESSING: {}", id);
            safeProcess(id);
        }
    }

    private void safeProcess(UUID id) {
        try {
            recordProcessor.processOne(id);
        } catch (Exception e) {
            log.error("Unexpected failure processing outbox record {} — continuing sweep", id, e);
        }
    }
}