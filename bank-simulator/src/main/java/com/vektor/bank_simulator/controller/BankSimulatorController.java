package com.vektor.bank_simulator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class BankSimulatorController {

    @PostMapping
    public ResponseEntity<BankResponse> processTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BankRequest request) throws InterruptedException {

        // The Chaos Monkey: Randomly inject failures into the API!
        double chance = Math.random();
        
        if (chance < 0.2) {
            // 20% chance the bank's servers crash
            System.out.println("INJECTING FAILURE: Returning 503 Service Unavailable");
            return ResponseEntity.status(503).build(); 
        } else if (chance < 0.4) {
            // 20% chance the bank is incredibly slow (triggers timeouts)
            System.out.println("INJECTING LATENCY: Sleeping for 4 seconds...");
            Thread.sleep(4000); 
        }

        // 60% chance of success!
        String bankRef = "BANK-REF-" + idempotencyKey.substring(0, 8).toUpperCase();
        System.out.println("SUCCESS: Processed transfer for " + request.driverId() + " -> " + bankRef);
        return ResponseEntity.ok(new BankResponse(bankRef, "SUCCESS"));
    }

    public record BankRequest(String driverId, Double amount) {}
    public record BankResponse(String bankReferenceId, String status) {}
}

// ### The Ultimate Demo
// Once you spin up the Simulator on `8081` and your Engine on `8080`, start firing your mock driver script. 

// Watch the engine logs! You will see the engine happily making HTTP calls, but when the chaos monkey hits the 20% failure rate, you will see Resilience4j actively **OPEN THE CIRCUIT**. The outbox processor will cleanly mark the records as `FAILED_SYSTEM_DOWN`, and 60 seconds later, your Background Sweeper will pick them back up and retry them with the exact same Idempotency Key!

// Let me know once you see the circuit breaker trip in your terminal!