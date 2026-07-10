package com.vektor.dispatch_engine.config;

import com.vektor.dispatch_engine.dto.common.AggregatedDelivery;
import com.vektor.dispatch_engine.model.DriverPayout;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.concurrent.Future;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class PayoutBatchConfig {
    private final DeliveryEventRepository deliveryEventRepository;
    private final DriverPayoutRepository driverPayoutRepository;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Payout-Worker-");
        executor.initialize();

        return executor;
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<AggregatedDelivery> deliveryEventReader() {
        return new JdbcCursorItemReaderBuilder<AggregatedDelivery>()
                .name("deliveryEventReader")
                .dataSource(dataSource)
                .sql("SELECT " +
                        "   e.driver_id, " +
                        "   COUNT(e.id) as delivery_count, " +
                        "   SUM(e.distance_km) as total_distance, " +
                        "   COALESCE(r.base_rate, 5.00) as base_rate, " +
                        "   COALESCE(r.per_km_rate, 1.50) as per_km_rate " +
                        "FROM delivery_events e " +
                        "LEFT JOIN driver_rate_cards r ON e.driver_id = r.driver_id " +
                        "WHERE e.processed = false AND e.status = 'DELIVERED' " +
                        "GROUP BY e.driver_id, r.base_rate, r.per_km_rate")
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

            BigDecimal baseTotal = aggregatedDelivery.baseRate().multiply(new BigDecimal(aggregatedDelivery.count()));
            BigDecimal distanceTotal = aggregatedDelivery.totalDistance().multiply(aggregatedDelivery.perKmRate());
            BigDecimal grandTotal = baseTotal.add(distanceTotal);
            return new DriverPayout(aggregatedDelivery.driverId(), grandTotal, aggregatedDelivery.count());
        };
    }

    @Bean
    public AsyncItemProcessor<AggregatedDelivery, DriverPayout> asyncProcessor() {
        AsyncItemProcessor<AggregatedDelivery, DriverPayout> asyncItemProcessor = new AsyncItemProcessor<>(
                driverPayoutProcessor());
        asyncItemProcessor.setTaskExecutor(batchTaskExecutor());

        return asyncItemProcessor;
    }

    @Bean
    public ItemWriter<DriverPayout> driverPayoutWriter() {
        return chunk -> {
            for (DriverPayout payout : chunk) {
                driverPayoutRepository.save(payout);
                deliveryEventRepository.markAsProcessedForDriver(payout.getDriverId());
            }
        };
    }

    @Bean
    public AsyncItemWriter<DriverPayout> asyncWriter() {
        AsyncItemWriter<DriverPayout> asyncItemWriter = new AsyncItemWriter<>(driverPayoutWriter());
        return asyncItemWriter;
    }

    @Bean
    public Step driverPayoutStep(JobRepository jobRepository) {
        return new StepBuilder("driverPayoutStep", jobRepository)
                .<AggregatedDelivery, Future<DriverPayout>>chunk(10)
                .reader(deliveryEventReader())
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job driverPayoutJob(JobRepository jobRepository, Step driverPayoutStep) {
        return new JobBuilder("driverPayoutJob", jobRepository)
                .start(driverPayoutStep)
                .build();
    }
}
