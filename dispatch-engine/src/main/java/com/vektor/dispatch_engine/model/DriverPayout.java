package com.vektor.dispatch_engine.model;

import jakarta.persistence.*;
import lombok.*;

import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.utils.UuidV7;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_payouts", indexes = {
        @Index(name = "idx_payout_driver", columnList = "driver_id"),
        @Index(name = "idx_payout_status", columnList = "status")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DriverPayout {

    @Id
    @UuidV7
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    // Precision 10, Scale 2 allows up to 99,999,999.99 (Perfect for currency)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private int deliveriesProcessed;

    private Instant payoutCalculatedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DriverPayoutStatus status = DriverPayoutStatus.PENDING;

    public DriverPayout(String driverId, BigDecimal totalAmount, int deliveriesProcessed) {
        this.driverId = driverId;
        this.totalAmount = totalAmount;
        this.deliveriesProcessed = deliveriesProcessed;
        this.payoutCalculatedAt = Instant.now();
    }
}