package com.vektor.dispatch_engine.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import jakarta.validation.constraints.*;

public record DriverPayoutResponse(
        @NotBlank(message = "Driver ID cannot be blank")
        @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters")
        String driverId,

        @NotNull(message = "Total amount cannot be null")
        @Positive(message = "Total amount must be positive")
        BigDecimal totalAmount,

        @NotNull(message = "Deliveries processed cannot be null")
        @Positive(message = "Deliveries processed count must be positive")
        Integer deliveriesProcessed,

        @NotNull(message = "Payout calculation timestamp cannot be null")
        @PastOrPresent(message = "Payout calculation timestamp cannot be in the future")
        Instant payoutCalculatedAt,

        @NotNull(message = "Payout status cannot be null")
        DriverPayoutStatus status) {
}
