package com.vektor.dispatch_engine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "delivery_events",
        indexes = {
                @Index(name = "idx_unprocessed", columnList = "processed, status"),
                @Index(name = "idx_driver", columnList = "driver_id")
        }
)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeliveryEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    @Column(nullable = false)
    private String status;
    private Double lat;
    private Double lng;
    private Instant receivedAt;
    private boolean processed = false;

}

