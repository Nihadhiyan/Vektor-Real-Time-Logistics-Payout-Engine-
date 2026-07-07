package com.vektor.dispatch_engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryEventUpdate(
        String driverId,
        String status,
        Double lat,
        Double lng
) {}
