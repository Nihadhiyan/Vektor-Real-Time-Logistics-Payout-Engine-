package com.vektor.dispatch_engine.processor;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vektor.dispatch_engine.dto.payout.response.PayoutCompletedEventPayload;
import com.vektor.dispatch_engine.gateway.BankGatewayService;
import com.vektor.dispatch_engine.model.PayoutOutbox;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.repository.PayoutOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRecordProcessor {

    private final PayoutOutboxRepository outboxRepository;
    private final BankGatewayService bankGatewayService;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${vektor.outbox.stuck-threshold-minutes:5}")
    private long stuckThresholdMinutes;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID outboxId) {
        Instant now = Instant.now();
        Instant stuckThreshold = now.minus(Duration.ofMinutes(stuckThresholdMinutes));

        int claimed = outboxRepository.claim(outboxId, now, stuckThreshold);
        if (claimed == 0) {
            log.debug("Outbox record {} not claimable (owned elsewhere or already resolved)", outboxId);
            return;
        }

        PayoutOutbox record = outboxRepository.findById(Objects.requireNonNull(outboxId))
                .orElseThrow(() -> new IllegalStateException("Missing record after successful claim"));

        try {
            String bankRef = bankGatewayService.executeTransfer(
                    record.getOutboxId(), record.getDriverId(), record.getTotalAmount());

            if ("FAILED_SYSTEM_DOWN".equals(bankRef)) {
                record.setStatus(DriverPayoutStatus.FAILED);
                log.warn("Transfer failed (circuit open) for outbox {}", outboxId);
                publishResult(record, DriverPayoutStatus.FAILED, null);
            } else {
                record.setStatus(DriverPayoutStatus.PAID);
                record.setBankReferenceId(bankRef);
                log.info("Transfer PAID for outbox {} ref {}", outboxId, bankRef);
                publishResult(record, DriverPayoutStatus.PAID, bankRef);
            }
        } catch (Exception e) {
            record.setStatus(DriverPayoutStatus.FAILED);
            log.error("Transfer error for outbox {}: {}", outboxId, e.getMessage());
            publishResult(record, DriverPayoutStatus.FAILED, null);
        }

        outboxRepository.save(record);
    }

    private void publishResult(PayoutOutbox record, DriverPayoutStatus status, String bankRef) {
        PayoutCompletedEventPayload payload = PayoutCompletedEventPayload.builder()
            .outboxId(record.getOutboxId())
            .driverId(record.getDriverId())
            .amount(record.getTotalAmount())
            .bankReference(bankRef)
            .status(status)
            .processedAt(Instant.now())
            .build();

        kafkaTemplate.send("payout-results", Objects.requireNonNull(record.getDriverId()), payload);
        log.debug("Published payout result to Kafka for driver: {}", record.getDriverId());
    }
}