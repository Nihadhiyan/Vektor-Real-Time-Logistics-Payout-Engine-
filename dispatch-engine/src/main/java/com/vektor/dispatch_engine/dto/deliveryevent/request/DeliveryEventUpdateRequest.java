package com.vektor.dispatch_engine.dto.deliveryevent.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryEventUpdateRequest(
                @NotNull(message = "Event ID cannot be null")
                UUID eventId,

                @NotBlank(message = "Driver ID cannot be blank")
                @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters")
                String driverId,

                @NotNull(message = "Status cannot be null")
                DeliveryEventStatus status,

                @Min(value = -90, message = "Latitude must be >= -90")
                @Max(value = 90, message = "Latitude must be <= 90")
                Double lat,

                @Min(value = -180, message = "Longitude must be >= -180")
                @Max(value = 180, message = "Longitude must be <= 180")
                Double lng,

                @PositiveOrZero(message = "Distance in km cannot be negative")
                Double distanceKm,

                @NotNull(message = "Occurred timestamp cannot be null")
                @PastOrPresent(message = "Event occurred timestamp cannot be in the future")
                Instant occurredAt) {
}
