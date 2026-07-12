package com.vektor.dispatch_engine.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "vektor.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
    
}
