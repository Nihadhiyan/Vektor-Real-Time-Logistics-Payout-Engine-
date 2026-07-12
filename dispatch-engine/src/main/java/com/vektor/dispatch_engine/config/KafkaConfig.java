package com.vektor.dispatch_engine.config;

import java.util.Map;
import java.util.Objects;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.NonNull;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, byte[]> dltKafkaTemplate(@Value("${spring.kafka.producer.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class
        );

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Objects.requireNonNull(props)));
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(@NonNull ProducerFactory<String, Object> producerFactory) {
        Map<String, Object> props = new java.util.HashMap<>(producerFactory.getConfigurationProperties());
        
        org.apache.kafka.common.serialization.Serializer<Object> valueSerializer = (topic, data) -> {
            if (data == null) return null;
            if (data instanceof byte[] bytes) return bytes;
            if (data instanceof String str) return str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try {
                return new org.springframework.kafka.support.serializer.JsonSerializer<Object>().serialize(topic, data);
            } catch (Exception e) {
                throw new org.apache.kafka.common.errors.SerializationException("Error serializing payload", e);
            }
        };

        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(props, new org.apache.kafka.common.serialization.StringSerializer(), valueSerializer);
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public DefaultErrorHandler errorHandler(@Qualifier("dltKafkaTemplate") KafkaTemplate<String, byte[]> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(Objects.requireNonNull(dltKafkaTemplate),
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        return new DefaultErrorHandler(Objects.requireNonNull(recoverer), new FixedBackOff(1000L, 3));
    }
}
