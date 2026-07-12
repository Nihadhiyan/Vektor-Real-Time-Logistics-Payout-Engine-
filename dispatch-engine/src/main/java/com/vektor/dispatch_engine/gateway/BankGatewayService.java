package com.vektor.dispatch_engine.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@Slf4j
public class BankGatewayService {
    private final RestClient bankRestClient;

    public BankGatewayService(@NonNull @Value("${vektor.bank.base-url:https://api.example-bank.com}") String baseUrl) {
        this.bankRestClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "bankGateway", fallbackMethod = "fallbackTransfer")
    public String executeTransfer(@NonNull UUID idempotencyKey, @NonNull String driverId, @NonNull BigDecimal amount) {

        log.info("Initiating bank transfer via external rails for Driver: {} | Amount: {} | IdempotencyKey: {}",
                driverId, amount, idempotencyKey);
        
        // When the bank simulator lands:
        // return bankRestClient.post()
        //         .uri("/transfers")
        //         .header("Idempotency-Key", idempotencyKey.toString())
        //         .body(new TransferRequest(driverId, amount))
        //         .retrieve()
        //         .body(String.class);
        
        return "BANK-REF-" + idempotencyKey.toString().substring(0, 8).toUpperCase();

    }

    public String fallbackTransfer(@NonNull UUID idempotencyKey, @NonNull String driverId, @NonNull BigDecimal amount, Throwable t) {
        log.error("Downstream Banking Rail unavailable. Circuit Breaker tripped for Driver: {}. Reason: {}",
                driverId, t.getMessage());
        return "FAILED_SYSTEM_DOWN";
    }
}
