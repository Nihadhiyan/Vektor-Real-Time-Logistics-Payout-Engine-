package com.vektor.dispatch_engine.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;

import lombok.Builder;

@Builder
public record PayoutCompletedEventPayload(
        UUID outboxId,
        String driverId,
        BigDecimal amount,
        String bankReference,
        DriverPayoutStatus status,
        Instant processedAt
) {
    
}
