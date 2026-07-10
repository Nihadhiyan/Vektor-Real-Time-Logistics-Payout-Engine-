package com.vektor.dispatch_engine.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.service.DriverPayoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class DeliveryPayoutController {

    private final DriverPayoutService driverPayoutService;

    @PostMapping("/trigger-settlement")
    public ResponseEntity<String> triggerSettlement() {
        return ResponseEntity.ok(driverPayoutService.triggerSettlement());
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<List<DriverPayoutResponse>> getDriverPayouts(@PathVariable String driverId,
            Pageable pageable) {
        return ResponseEntity.ok(driverPayoutService.getDriverPayouts(driverId, pageable));
    }
}
