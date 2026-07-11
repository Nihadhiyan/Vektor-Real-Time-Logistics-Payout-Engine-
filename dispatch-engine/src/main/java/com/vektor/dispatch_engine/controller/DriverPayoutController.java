package com.vektor.dispatch_engine.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.vektor.dispatch_engine.dto.payout.request.DriverPayoutRequest;
import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.service.DriverPayoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@Validated
public class DriverPayoutController {

    private final DriverPayoutService driverPayoutService;

    @PostMapping("/trigger-settlement")
    public ResponseEntity<String> triggerSettlement(@Valid @RequestBody DriverPayoutRequest request) {
        return ResponseEntity.ok(driverPayoutService.triggerSettlement());
    }

    @PostMapping("/calculate")
    public ResponseEntity<DriverPayoutResponse> calculatePayout(@Valid @RequestBody DriverPayoutRequest request) {
        return ResponseEntity.ok(driverPayoutService.calculatePayout(request));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<List<DriverPayoutResponse>> getDriverPayouts(
            @PathVariable @NotBlank(message = "Driver ID cannot be blank") @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters") String driverId,
            Pageable pageable) {
        return ResponseEntity.ok(driverPayoutService.getDriverPayouts(driverId, pageable));
    }

    @GetMapping("/driver/{driverId}/status")
    public ResponseEntity<String> getDriverStatus(
            @PathVariable @NotBlank(message = "Driver ID cannot be blank") @Size(min = 2, max = 64, message = "Driver ID must be between 2 and 64 characters") String driverId) {
        // Placeholder for future repository query
        return ResponseEntity.ok("Status endpoint active for driver: " + driverId);
    }
}
