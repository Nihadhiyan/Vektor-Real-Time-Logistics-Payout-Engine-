package com.vektor.dispatch_engine.model;

import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_events", indexes = {
                @Index(name = "idx_unprocessed", columnList = "processed, status"),
                @Index(name = "idx_driver", columnList = "driver_id")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeliveryEvent {

        @Id
        @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
        private UUID id;

        @Column(nullable = false, unique = true)
        private UUID eventId;

        @Column(name = "driver_id", nullable = false)
        private String driverId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private DeliveryEventStatus status;

        private Double lat;
        private Double lng;

        @Column(name = "distance_km")
        private Double distanceKm;

        @Column(nullable = false)
        private Instant receivedAt;

        @Column(nullable = false)
        private Instant occurredAt;

        private boolean processed = false;

}
