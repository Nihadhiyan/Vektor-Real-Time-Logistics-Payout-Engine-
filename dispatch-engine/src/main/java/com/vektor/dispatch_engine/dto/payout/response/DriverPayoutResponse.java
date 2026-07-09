package com.vektor.dispatch_engine.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;

public record DriverPayoutResponse(
        String driverId,
        BigDecimal totalAmount,
        Integer deliveriesProcessed,
        Instant payoutCalculatedAt,
        DriverPayoutStatus status) {
}
