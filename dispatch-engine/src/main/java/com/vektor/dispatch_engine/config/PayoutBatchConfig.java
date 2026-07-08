package com.vektor.dispatch_engine.config;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import com.vektor.dispatch_engine.model.DriverPayout;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PayoutBatchConfig {
    private final DeliveryEventRepository deliveryEventRepository;
    private  final DriverPayoutRepository driverPayoutRepository;

    @Bean
    public RepositoryItemReader<DeliveryEvent> deliveryEventReader() {
        return new RepositoryItemReaderBuilder<DeliveryEvent>()
                .name("deliveryEventReader")
                .repository(deliveryEventRepository)
                .methodName("findByProcessedFalseAndStatus")
                .arguments(List.of("DELIVERED"))
                .sorts(Map.of("receivedAt", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<DeliveryEvent, DriverPayout> driverPayoutProcessor() {
        return deliveryEvent -> {
            deliveryEvent.setProcessed(true);
            deliveryEventRepository.save(deliveryEvent);

            return new DriverPayout(deliveryEvent.getDriverId(), new BigDecimal("5.00"), 1);
        };
    }

    @Bean
    public RepositoryItemWriter<DriverPayout> driverPayoutWriter() {
        return new RepositoryItemWriterBuilder<DriverPayout>()
                .repository(driverPayoutRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step driverPayoutStep(JobRepository jobRepository) {
        return new StepBuilder("driverPayoutStep", jobRepository)
                .<DeliveryEvent, DriverPayout>chunk(10)
                .reader(deliveryEventReader())
                .processor(driverPayoutProcessor())
                .writer(driverPayoutWriter())
                .build();
    }

    @Bean
    public Job driverPayoutJob(JobRepository jobRepository, Step driverPayoutStep) {
        return new JobBuilder("driverPayoutJob", jobRepository)
                .start(driverPayoutStep)
                .build();
    }
}
