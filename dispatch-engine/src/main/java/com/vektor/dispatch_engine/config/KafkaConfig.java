package com.vektor.dispatch_engine.config;

import java.util.Objects;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(Objects.requireNonNull(kafkaTemplate),
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        return new DefaultErrorHandler(Objects.requireNonNull(recoverer), new FixedBackOff(1000L, 3));
    }
}
