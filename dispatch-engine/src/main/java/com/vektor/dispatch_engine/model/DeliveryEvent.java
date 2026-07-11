package com.vektor.dispatch_engine.model;

import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import com.vektor.dispatch_engine.utils.UuidV7;

import jakarta.persistence.*;
import lombok.*;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
        @UuidV7
        private UUID id;

        @Column(nullable = false, unique = true)
        @NonNull
        private UUID eventId;

        @Column(name = "driver_id", nullable = false)
        @NonNull
        private String driverId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @NonNull
        private DeliveryEventStatus status;

        private Double lat;
        private Double lng;

        @Column(name = "distance_km")
        @NonNull
        private Double distanceKm;

        @Column(nullable = false)
        @NonNull
        private Instant receivedAt;

        @Column(nullable = false)
        @NonNull
        private Instant occurredAt;

        private boolean processed = false;

}
