package com.vektor.dispatch_engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryUpdate(
        String driverId,
        String status,
        Double lat,
        Double lng
) {}
