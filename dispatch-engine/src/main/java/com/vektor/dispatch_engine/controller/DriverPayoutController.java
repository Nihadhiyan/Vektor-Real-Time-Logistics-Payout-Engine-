package com.vektor.dispatch_engine.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.dto.payout.response.SettlementTriggerResponse;
import com.vektor.dispatch_engine.service.DriverPayoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@Validated
public class DriverPayoutController {

    private final DriverPayoutService driverPayoutService;

    @Operation(summary = "Trigger manual settlement", description = "Triggers a manual settlement process for drivers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settlement triggered or already completed for this cutoff"),
        @ApiResponse(responseCode = "500", description = "Job launch failure", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        
    })
    @PostMapping("/trigger-settlement")
    public ResponseEntity<SettlementTriggerResponse> triggerSettlement() {
        return ResponseEntity.ok(driverPayoutService.triggerSettlement());
    }

    @Operation(summary = "Get driver payouts", description = "Fetches all payouts for a specific driver")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payout statements, newest first"),
        @ApiResponse(responseCode = "400", description = "Invalid driver ID", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
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
