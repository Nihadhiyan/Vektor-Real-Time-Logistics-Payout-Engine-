package com.vektor.dispatch_engine;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.kafka.clients.consumer.Consumer;
import java.lang.System;
import java.math.BigDecimal;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.model.DeliveryEvent;
import com.vektor.dispatch_engine.model.PayoutOutbox;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.processor.OutboxRecordProcessor;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;
import com.vektor.dispatch_engine.repository.PayoutOutboxRepository;

@SpringBootTest(properties = { "vektor.payout.schedule-ms=2000", "vektor.outbox.sweep-ms=2000" })
@SpringBatchTest
@Testcontainers
public class DispatchEngineFailureIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.0")
            .withDatabaseName("vektor_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = 
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("vektor.redis.url", () -> "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private DeliveryEventRepository deliveryEventRepository;

    @Autowired
    private DriverPayoutRepository driverPayoutRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job driverPayoutJob;

    @Autowired
    private PayoutOutboxRepository outboxRepository;

    @Autowired
    private OutboxRecordProcessor recordProcessor;

    

    // --- TEST 1: IDEMPOTENCY ---

    @Test
    void shouldEnforceIdempotencyAndBlockDuplicateEvents() {
        String driverId = "DUPLICATE-TEST-DRIVER";
        UUID sharedEventId = UUID.randomUUID();

        DeliveryEventUpdateRequest request1 = new DeliveryEventUpdateRequest(
                sharedEventId, driverId, DeliveryEventStatus.DELIVERED, 6.820, 79.880, 5.5, Instant.now());

        DeliveryEventUpdateRequest request2 = new DeliveryEventUpdateRequest(
                sharedEventId, driverId, DeliveryEventStatus.DELIVERED, 6.820, 79.880, 5.5, Instant.now());

        kafkaTemplate.send("delivery-updates", driverId, request1);
        kafkaTemplate.send("delivery-updates", driverId, request2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = deliveryEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(sharedEventId))
                    .count();

            assertThat(count).isEqualTo(1);
        });

    }

    // --- TEST 2: DEAD LETTER TOPIC (POISON PILL) ---

    @Test
    void shouldRoutePoisonPillToDlt() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-dlt-group",
                "false");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        Consumer<String, String> dltConsumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                .createConsumer();
        dltConsumer.subscribe(List.of("delivery-updates-dlt"));

        String poisonPill = "{\"driverId\": \"R-101\", \"lat\": \"THIS_WILL_CRASH_JAVA\"}";
        kafkaTemplate.send("delivery-updates", "R-101", poisonPill);

        ConsumerRecord<String, String> dltRecord = KafkaTestUtils.getSingleRecord(dltConsumer, "delivery-updates-dlt",
                Duration.ofSeconds(10));

        String receivedValue = dltRecord.value();

        assertThat(receivedValue).isEqualTo(poisonPill);

        dltConsumer.close();

    }

    // --- TEST 3: BATCH RESTART / NO DOUBLE PAYMENTS ---

    @Test
    void shouldNotDoublePayOnJobRestart() throws Exception {
        String driverId = "RESTART-DRIVER";

        DeliveryEvent event = new DeliveryEvent();
        event.setEventId(UUID.randomUUID());
        event.setDriverId(driverId);
        event.setStatus(DeliveryEventStatus.DELIVERED);
        event.setDistanceKm(10.0);
        event.setProcessed(false);
        event.setOccurredAt(Instant.now());
        event.setReceivedAt(Instant.now());
        deliveryEventRepository.save(event);

        JobExecution run1 = jobLauncher
                .run(Objects.requireNonNull(driverPayoutJob), new JobParametersBuilder()
                        .addString("cutoff", Instant.now().toString())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(run1.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        long payoutsAfterRun1 = driverPayoutRepository.findAll().stream().filter(p -> p.getDriverId().equals(driverId))
                .count();
        assertThat(payoutsAfterRun1).isEqualTo(1);

        JobExecution run2 = jobLauncher
                .run(Objects.requireNonNull(driverPayoutJob), new JobParametersBuilder()
                        .addString("cutoff", Instant.now().toString())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(run2.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        long payoutsAfterRun2 = driverPayoutRepository.findAll().stream().filter(p -> p.getDriverId().equals(driverId))
                .count();
        assertThat(payoutsAfterRun2).isEqualTo(1);

    }


    @Test
    void shouldRecoverStuckProcessingRecord() {
        PayoutOutbox stuck = new PayoutOutbox();
        stuck.setDriverId("STUCK-DRIVER");
        stuck.setTotalAmount(new BigDecimal("42.00"));
        stuck.setStatus(DriverPayoutStatus.PROCESSING);
        stuck.setClaimedAt(Instant.now().minus(Duration.ofMinutes(10)));
        stuck = outboxRepository.save(stuck);

        recordProcessor.processOne(stuck.getOutboxId());

        PayoutOutbox recovered = outboxRepository.findById(Objects.requireNonNull(stuck.getOutboxId())).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(DriverPayoutStatus.PAID);
        assertThat(recovered.getBankReferenceId()).isNotBlank();
    }

    @Test
    void shouldNotReclaimFreshProcessingRecord() {
        PayoutOutbox inFlight = new PayoutOutbox();
        inFlight.setDriverId("INFLIGHT-DRIVER");
        inFlight.setTotalAmount(new BigDecimal("10.00"));
        inFlight.setStatus(DriverPayoutStatus.PROCESSING);
        inFlight.setClaimedAt(Instant.now());                 // just claimed by "someone else"
        inFlight = outboxRepository.save(inFlight);

        recordProcessor.processOne(inFlight.getOutboxId());

        PayoutOutbox unchanged = outboxRepository.findById(Objects.requireNonNull(inFlight.getOutboxId())).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(DriverPayoutStatus.PROCESSING);  // untouched
        assertThat(unchanged.getBankReferenceId()).isNull();
    }

}
