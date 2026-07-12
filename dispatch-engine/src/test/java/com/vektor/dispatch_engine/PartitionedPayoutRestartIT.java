package com.vektor.dispatch_engine;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import com.vektor.dispatch_engine.model.DriverPayout;
import com.vektor.dispatch_engine.model.enums.DeliveryEventStatus;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest
@Testcontainers
public class PartitionedPayoutRestartIT {
    
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

    static final String POISONED_DRIVER = "FAIL-DRIVER";
    static final AtomicBoolean FAILURE_ARMED = new AtomicBoolean(false);

    @TestConfiguration
    static class FailingWriterConfig {

        public ItemWriter<DriverPayout> failingPayoutWriter(
            @Qualifier("driverPayoutWriter") ItemWriter<DriverPayout> realWriter
        ) {
            return chunk -> {
                if(FAILURE_ARMED.get()) {
                    for (DriverPayout p : chunk) {
                        if (POISONED_DRIVER.equals(p.getDriverId())) {
                            throw new IllegalStateException("Injected failure for " + POISONED_DRIVER);
                        }
                    }
                }
                realWriter.write(chunk);
            };
        }
    }

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job driverPayoutJob;

    @Autowired
    DeliveryEventRepository deliveryEventRepository;

    @Autowired
    DriverPayoutRepository driverPayoutRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${vektor.payout.partition-count:4}")
    int gridSize;

    @Test
    void failedPartitionRestartsWithoutDoublePayingAnyDriver() throws Exception {
        // --- Seed: enough drivers that every bucket is statistically occupied ---
        List<String> drivers = new ArrayList<>();
        drivers.add(POISONED_DRIVER);
        for (int i = 0; i < 19; i++) drivers.add("DRIVER-" + i);
        drivers.forEach(d -> seedDeliveredEvent(d, 5.0));

        // --- Ask Postgres which bucket each driver belongs to (exact, not guessed) ---
        Map<String, Integer> bucketOf = new HashMap<>();
        for (String d : drivers) {
            bucketOf.put(d, jdbcTemplate.queryForObject(
                    "SELECT abs(hashtext(?)) % ?", Integer.class, d, gridSize));
        }
        int poisonedBucket = bucketOf.get(POISONED_DRIVER);
        // Sanity: the test only proves something if other buckets have work
        assertThat(bucketOf.values().stream().distinct().count()).isGreaterThan(1);

        String cutoff = Instant.now().toString();

        // --- Run 1: armed → poisoned bucket fails, others complete ---
        FAILURE_ARMED.set(true);
        JobExecution run1 = jobLauncher.run(driverPayoutJob, params(cutoff));

        assertThat(run1.getStatus()).isEqualTo(BatchStatus.FAILED);

        Set<String> paidAfterRun1 = paidDrivers();
        // No driver in the poisoned bucket's failed chunk got paid...
        assertThat(paidAfterRun1).doesNotContain(POISONED_DRIVER);
        // ...but at least one driver from a *different* bucket did (partition isolation)
        assertThat(paidAfterRun1.stream().anyMatch(d -> bucketOf.get(d) != poisonedBucket)).isTrue();
        // And nobody is paid twice
        assertNoDoublePayments();

        // --- Run 2: disarmed, IDENTICAL cutoff → Batch restarts only the failed partition ---
        FAILURE_ARMED.set(false);
        JobExecution run2 = jobLauncher.run(driverPayoutJob, params(cutoff));

        assertThat(run2.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Every seeded driver paid exactly once; no unprocessed DELIVERED events remain before cutoff
        assertThat(paidDrivers()).containsExactlyInAnyOrderElementsOf(drivers);
        assertNoDoublePayments();
        assertThat(unprocessedDeliveredCount(Instant.parse(cutoff))).isZero();

        // Bonus assertion: completed partitions were NOT re-executed on restart
        long workerExecutionsRun2 = run2.getStepExecutions().stream()
                .filter(se -> se.getStepName().startsWith("workerPayoutStep:"))
                .count();
        assertThat(workerExecutionsRun2).isLessThan(gridSize); // only failed bucket(s) re-ran
    }

    private JobParameters params(String cutoff) {
        return new JobParametersBuilder().addString("cutoff", cutoff).toJobParameters();
    }

    private Set<String> paidDrivers() {
        return driverPayoutRepository.findAll().stream()
                .map(DriverPayout::getDriverId).collect(Collectors.toSet());
    }

    private void assertNoDoublePayments() {
        Map<String, Long> counts = driverPayoutRepository.findAll().stream()
                .collect(Collectors.groupingBy(DriverPayout::getDriverId, Collectors.counting()));
        assertThat(counts.values()).allMatch(c -> c == 1L);
    }

    private void seedDeliveredEvent(String driverId, double distanceKm) {
        DeliveryEvent event = new DeliveryEvent();
        event.setEventId(UUID.randomUUID());
        event.setDriverId(driverId);
        event.setStatus(DeliveryEventStatus.DELIVERED);
        event.setDistanceKm(distanceKm);
        event.setProcessed(false);
        event.setOccurredAt(Instant.now());
        event.setReceivedAt(Instant.now());
        deliveryEventRepository.save(event);
    }

    private long unprocessedDeliveredCount(Instant cutoff) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM delivery_events WHERE processed = false AND status = 'DELIVERED' AND received_at <= ?::timestamptz",
                Long.class, cutoff.toString());
        return count != null ? count : 0L;
    }
}
