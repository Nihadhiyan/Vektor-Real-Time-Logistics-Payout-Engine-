package com.vektor.dispatch_engine;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "vektor.payout.schedule-ms=2000", "vektor.outbox.sweep-ms=2000" })
@Testcontainers
@TestPropertySource(properties = "vektor.scheduling.enabled=true")
public class DispatchEngineIntegrationTest {

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
    static org.testcontainers.containers.GenericContainer<?> redis =
        new org.testcontainers.containers.GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

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
    private Job driverPayoutJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

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

    @Test
    void shouldPublishPayoutResultToKafka() throws Exception {
        // 1. Create a consumer and subscribe to your new topic
        Consumer<String, String> consumer = consumerFactory.createConsumer("test-group", "test-client");
        consumer.subscribe(java.util.Collections.singletonList("payout-results"));
        
        // 2. Trigger your batch job (just like you did in the other tests)
        String cutoff = Instant.now().toString();

        jobLauncher.run(driverPayoutJob, new JobParametersBuilder()
                .addString("cutoff", cutoff)
                .toJobParameters());

        // 3. Wait for the message to arrive in Kafka (max 10 seconds)
        ConsumerRecord<String, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "payout-results", Duration.ofMillis(10000));

        // 4. Assert the contents!
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.key()).isEqualTo("R-101"); // Partition key should be driverId
        assertThat(singleRecord.value()).contains("\"status\":\"PAID\"");
        
        consumer.close();
    }

}