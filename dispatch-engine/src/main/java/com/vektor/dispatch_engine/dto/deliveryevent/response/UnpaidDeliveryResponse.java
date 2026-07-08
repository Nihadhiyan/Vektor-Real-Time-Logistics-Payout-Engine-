package com.vektor.dispatch_engine.dto.deliveryevent.response;

import java.time.Instant;
import java.util.UUID;

public record UnpaidDeliveryResponse(
        UUID eventId,
        String driverId,
        String status,
        Instant occurredAt,
        Instant receivedAt) {
}
