package com.vektor.dispatch_engine.config;

import com.vektor.dispatch_engine.dto.common.AggregatedDelivery;
import com.vektor.dispatch_engine.event.PayoutOutboxCreatedEvent;
import com.vektor.dispatch_engine.model.DriverPayout;
import com.vektor.dispatch_engine.model.PayoutOutbox;
import com.vektor.dispatch_engine.model.enums.DriverPayoutStatus;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;
import com.vektor.dispatch_engine.repository.PayoutOutboxRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class PayoutBatchConfig {
    private final DeliveryEventRepository deliveryEventRepository;
    private final DriverPayoutRepository driverPayoutRepository;
    private final PayoutOutboxRepository payoutOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    @Bean
    @StepScope
    public JdbcCursorItemReader<AggregatedDelivery> deliveryEventReader(@Value("#{jobParameters['cutoff']}") String cutoff) {

        return new JdbcCursorItemReaderBuilder<AggregatedDelivery>()
                .name("deliveryEventReader")
                .dataSource(Objects.requireNonNull(dataSource))
                .sql("""
                    SELECT e.driver_id,
                        COUNT(e.id) AS delivery_count,
                        SUM(e.distance_km) AS total_distance,
                        COALESCE(r.base_rate, 5.00) AS base_rate,
                        COALESCE(r.per_km_rate, 1.50) AS per_km_rate
                    FROM delivery_events e
                    LEFT JOIN driver_rate_cards r ON e.driver_id = r.driver_id
                    WHERE e.processed = false
                    AND e.status = 'DELIVERED'
                    AND e.received_at <= ?::timestamptz
                    GROUP BY e.driver_id, r.base_rate, r.per_km_rate
                    """)
                .preparedStatementSetter((ps) -> ps.setString(1, cutoff))
                .rowMapper((rs, rowNum) -> new AggregatedDelivery(
                        rs.getString("driver_id"),
                        rs.getInt("delivery_count"),
                        rs.getBigDecimal("total_distance"),
                        rs.getBigDecimal("base_rate"),
                        rs.getBigDecimal("per_km_rate")))
                .build();
    }

    @Bean
    public ItemProcessor<AggregatedDelivery, DriverPayout> driverPayoutProcessor() {
        return aggregatedDelivery -> {

            // Math: (Base Rate * Count) + (Total Distance * Per-Km Rate)

            BigDecimal baseTotal = aggregatedDelivery.baseRate().multiply(BigDecimal.valueOf(aggregatedDelivery.count()));
            BigDecimal distanceTotal = aggregatedDelivery.totalDistance().multiply(aggregatedDelivery.perKmRate());
            BigDecimal grandTotal = baseTotal.add(distanceTotal);
            return new DriverPayout(aggregatedDelivery.driverId(), grandTotal, aggregatedDelivery.count());
        };
    }

    @Bean
    @StepScope
    public ItemWriter<DriverPayout> driverPayoutWriter(@Value("#{jobParameters['cutoff']}") String cutoff) {

        Instant cutoffInstant = Instant.parse(cutoff);

        return chunk -> {
            for (DriverPayout payout : chunk) {
                driverPayoutRepository.save(Objects.requireNonNull(payout));
                int updated = deliveryEventRepository.markAsProcessedForDriver(payout.getDriverId(), cutoffInstant);

                if (updated != payout.getDeliveriesProcessed()) {
                    throw new IllegalStateException(
                        "Settlement invariant violated for driver %s: paid %d deliveries but marked %d as processed"
                            .formatted(payout.getDriverId(), payout.getDeliveriesProcessed(), updated));
                }

                // === SAVE TO OUTBOX ===
                PayoutOutbox outbox = new PayoutOutbox();
                outbox.setDriverId(payout.getDriverId());
                outbox.setTotalAmount(payout.getTotalAmount());
                outbox.setStatus(DriverPayoutStatus.PENDING); // Ready for Dispatcher to send
                payoutOutboxRepository.save(outbox);
            }

            eventPublisher.publishEvent(new PayoutOutboxCreatedEvent(this));
        };
    }

    @Bean
    public Step driverPayoutStep(JobRepository jobRepository, JdbcCursorItemReader<AggregatedDelivery> deliveryEventReader, ItemProcessor<AggregatedDelivery, DriverPayout> driverPayoutProcessor, ItemWriter<DriverPayout> driverPayoutWriter) {
        return new StepBuilder("driverPayoutStep", Objects.requireNonNull(jobRepository))
                .<AggregatedDelivery, DriverPayout>chunk(10, Objects.requireNonNull(transactionManager))
                .reader(Objects.requireNonNull(deliveryEventReader))
                .processor(Objects.requireNonNull(driverPayoutProcessor))
                .writer(Objects.requireNonNull(driverPayoutWriter))
                .build();
    }

    @Bean
    public Job driverPayoutJob(JobRepository jobRepository, Step driverPayoutStep) {
        return new JobBuilder("driverPayoutJob", Objects.requireNonNull(jobRepository))
                .start(Objects.requireNonNull(driverPayoutStep))
                .build();
    }
}
