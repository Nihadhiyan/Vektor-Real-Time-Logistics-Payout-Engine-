package com.vektor.dispatch_engine.dto.payout.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DriverPayoutRequest(
        @NotBlank(message = "Driver ID cannot be blank")
        @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters")
        String driverId,

        @NotNull(message = "Total amount cannot be null")
        @Positive(message = "Total amount must be greater than zero")
        BigDecimal totalAmount,

        @NotNull(message = "Deliveries processed cannot be null")
        @Positive(message = "Deliveries processed count must be greater than zero")
        Integer deliveriesProcessed) {
}
