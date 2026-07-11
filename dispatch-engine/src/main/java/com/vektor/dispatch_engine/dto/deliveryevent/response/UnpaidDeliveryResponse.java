package com.vektor.dispatch_engine.dto.deliveryevent.response;

import java.time.Instant;
import java.util.UUID;

import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import jakarta.validation.constraints.*;

public record UnpaidDeliveryResponse(
                @NotNull(message = "Event ID cannot be null")
                UUID eventId,

                @NotBlank(message = "Driver ID cannot be blank")
                @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters")
                String driverId,

                @NotNull(message = "Status cannot be null")
                DeliveryEventStatus status,

                @NotNull(message = "Occurred timestamp cannot be null")
                @PastOrPresent(message = "Event occurred timestamp cannot be in the future")
                Instant occurredAt,

                @NotNull(message = "Received timestamp cannot be null")
                @PastOrPresent(message = "Event received timestamp cannot be in the future")
                Instant receivedAt) {
}
