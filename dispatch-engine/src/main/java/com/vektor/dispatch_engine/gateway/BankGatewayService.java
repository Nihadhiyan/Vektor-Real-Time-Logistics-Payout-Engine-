package com.vektor.dispatch_engine.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestClient;

import com.vektor.dispatch_engine.dto.bank.request.BankRequest;
import com.vektor.dispatch_engine.dto.bank.response.BankResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@Service
@Slf4j
public class BankGatewayService {
    private final RestClient bankRestClient;

    public BankGatewayService(@NonNull @Value("${vektor.bank.base-url:http://localhost:8085}") String baseUrl) {
        this.bankRestClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @RateLimiter(name = "bankGateway", fallbackMethod = "fallbackTransfer")
    @CircuitBreaker(name = "bankGateway", fallbackMethod = "fallbackTransfer")
    public String executeTransfer(@NonNull UUID idempotencyKey, @NonNull String driverId, @NonNull BigDecimal amount) {

        log.info("Initiating bank transfer via external rails for Driver: {} | Amount: {} | IdempotencyKey: {}",
                driverId, amount, idempotencyKey);
        
        BankResponse response = bankRestClient.post()
                .uri("/api/v1/transfers")
                .header("Idempotency-Key", idempotencyKey.toString())
                .body(new BankRequest(driverId, amount))
                .retrieve()
                .body(BankResponse.class);
        
        return response != null ? response.bankReferenceId() : "UNKNOWN";

    }

    public String fallbackTransfer(@NonNull UUID idempotencyKey, @NonNull String driverId, @NonNull BigDecimal amount, Throwable t) {
        if (t instanceof RequestNotPermitted) {
            log.warn("Downstream Banking Rail rate limit exceeded for Driver: {}. Reason: {}", driverId, t.getMessage());
        } else {
            log.error("Downstream Banking Rail unavailable. Circuit Breaker tripped or failure for Driver: {}. Reason: {}",
                    driverId, t.getMessage());
        }
        return "FAILED_SYSTEM_DOWN";
    }
}
