package com.vektor.dispatch_engine.processor;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.vektor.dispatch_engine.event.PayoutOutboxCreatedEvent;
import com.vektor.dispatch_engine.gateway.BankGatewayService;
import com.vektor.dispatch_engine.model.PayoutOutbox;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.repository.PayoutOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final BankGatewayService bankGatewayService;
    private final PayoutOutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutboxEvent(PayoutOutboxCreatedEvent payoutCreatedEvent) {
        log.info("WAKER TRIGGERED: New outbox event detected. Processing immediately.");
        processPendingOutboxRecords();
    }

    @Scheduled(fixedDelay = 15000)
    public void scheduledSweep() {
        log.debug("SCHEDULED SWEEP: Checking for orphaned outbox records...");
        processPendingOutboxRecords();
    }

    @Transactional
    public synchronized void processPendingOutboxRecords() {
        List<PayoutOutbox> pendingRecords = outboxRepository.findByStatus(DriverPayoutStatus.PENDING);

        if (pendingRecords.isEmpty()) {
            return;
        }

        for (PayoutOutbox record : pendingRecords) {
            try {
                record.setStatus(DriverPayoutStatus.PROCESSING);
                outboxRepository.saveAndFlush(record);

                String bankRef = bankGatewayService.executeTransfer(record.getDriverId(), record.getTotalAmount());

                if ("FAILED_SYSTEM_DOWN".equals(bankRef)) {
                    record.setStatus(DriverPayoutStatus.FAILED);
                } else {
                    record.setStatus(DriverPayoutStatus.PAID);
                    record.setBankReferenceId(bankRef);
                }
            } catch (Exception e) {
                log.error("Failed to process outbox record {}: {}", record.getOutboxId(), e.getMessage());
                record.setStatus(DriverPayoutStatus.FAILED);
            }

            outboxRepository.save(record);
        }
    }

}
