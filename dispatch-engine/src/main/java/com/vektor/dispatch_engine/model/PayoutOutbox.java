package com.vektor.dispatch_engine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.utils.UuidV7;

@Entity
@Table(name = "payout_outbox")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PayoutOutbox {

    @Id
    @UuidV7
    private UUID outboxId;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DriverPayoutStatus status;

    private String bankReferenceId;

    @PastOrPresent
    @Column(nullable = false)
    private Instant claimedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}