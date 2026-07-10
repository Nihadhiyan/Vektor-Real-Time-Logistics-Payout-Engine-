package com.vektor.dispatch_engine.dto.deliveryevent.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryEventUpdateRequest(
                UUID eventId,
                String driverId,
                DeliveryEventStatus status,
                Double lat,
                Double lng,
                Double distanceKm,
                Instant occurredAt) {
}
