package com.vektor.dispatch_engine.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverPayoutResponse(
                String driverId,
                BigDecimal totalAmount,
                Integer deliveriesProcessed,
                Instant payoutCalculatedAt,
                String status) {
}
