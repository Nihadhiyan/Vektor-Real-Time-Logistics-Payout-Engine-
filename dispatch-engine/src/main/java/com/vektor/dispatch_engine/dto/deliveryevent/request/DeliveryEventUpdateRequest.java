package com.vektor.dispatch_engine.dto.deliveryevent.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryEventUpdateRequest(
        String driverId,
        String status,
        Double lat,
        Double lng
) {}
