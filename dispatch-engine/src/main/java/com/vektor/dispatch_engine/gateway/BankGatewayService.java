package com.vektor.dispatch_engine.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@Slf4j
public class BankGatewayService {
    private final WebClient bankWebClient;

    public BankGatewayService() {
        this.bankWebClient = WebClient.builder().baseUrl("https://api.example-bank.com/transfers").build();
    }

    @CircuitBreaker(name = "bankGateway", fallbackMethod = "fallbackTransfer")
    public String executeTransfer(String driverId, BigDecimal amount) {
        log.info("Initiating bank transfer via external rails for Driver: {} | Amount: {}", driverId, amount);

        // return WebClient.post()
        // .uri("/transfers")
        // .header("Idempotency-Key", UUID.randomUUID().toString())
        // .bodyValue(new TransferRequest(driverId, amount))
        // .retrieve()
        // .bodyToMono(String.class)
        // .block();

        return "BANK-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    }

    public String fallbackTransfer(String driverId, BigDecimal amount, Throwable t) {
        log.error("Downstream Banking Rail unavailable. Circuit Breaker tripped for Driver: {}. Reason: {}",
                driverId, t.getMessage());
        return "FAILED_SYSTEM_DOWN";
    }
}
