package com.vektor.dispatch_engine;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class DispatchEngineIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.0")
            .withDatabaseName("vektor_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, DeliveryEventUpdateRequest> kafkaTemplate;

    @Autowired
    private DeliveryEventRepository deliveryEventRepository;

    @Autowired
    private DriverPayoutRepository driverPayoutRepository;

    @Test
    void shouldProcessDeliveryAndGeneratePayoutSuccessfully() {
        String driverId = "TEST-DRIVER-999";
        UUID eventId = UUID.randomUUID();

        DeliveryEventUpdateRequest request = new DeliveryEventUpdateRequest(
                eventId,
                driverId,
                DeliveryEventStatus.DELIVERED,
                6.820,
                79.880,
                5.5,
                Instant.now());

        kafkaTemplate.send("delivery-updates", driverId, request);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {

            var completedEvent = deliveryEventRepository.findByEventId(eventId);
            assertThat(completedEvent).isPresent();
            assertThat(completedEvent.get().isProcessed()).isTrue();

            var payouts = driverPayoutRepository
                    .findByDriverIdOrderByPayoutCalculatedAtDesc(driverId, Pageable.ofSize(1)).toList();
            assertThat(payouts).isNotEmpty();
            assertThat(payouts.get(0).getTotalAmount().doubleValue()).isEqualTo(9.6);
            assertThat(payouts.get(0).getStatus()).isEqualTo(DriverPayoutStatus.PENDING);
        });
    }

}