package com.vektor.dispatch_engine.dto.deliveryevent.response;

import java.time.Instant;
import java.util.UUID;

import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;

public record UnpaidDeliveryResponse(
                UUID eventId,
                String driverId,
                DeliveryEventStatus status,
                Instant occurredAt,
                Instant receivedAt) {
}
