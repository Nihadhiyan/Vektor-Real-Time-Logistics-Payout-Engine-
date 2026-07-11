package com.vektor.dispatch_engine.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.dto.deliveryevent.response.UnpaidDeliveryResponse;
import com.vektor.dispatch_engine.service.DeliveryEventService;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@Validated
public class DeliveryEventController {

    private final DeliveryEventService deliveryEventService;

    @GetMapping("/unpaid")
    public ResponseEntity<List<UnpaidDeliveryResponse>> getUnpaidDeliveries(Pageable pageable) {
        return ResponseEntity.ok(deliveryEventService.getUnpaidDeliveries(pageable));
    }

    @PostMapping("/events")
    public ResponseEntity<UnpaidDeliveryResponse> ingestDeliveryEvent(@Valid @RequestBody DeliveryEventUpdateRequest request) {
        return ResponseEntity.ok(deliveryEventService.ingestEvent(request));
    }

}
