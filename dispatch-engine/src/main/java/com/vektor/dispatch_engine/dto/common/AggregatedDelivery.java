package com.vektor.dispatch_engine.dto.common;

import java.math.BigDecimal;

public record AggregatedDelivery(
        String driverId,
        int count,
        BigDecimal totalDistance,
        BigDecimal baseRate,
        BigDecimal perKmRate) {
}
