package com.vektor.dispatch_engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutJobScheduler {
    private final JobLauncher jobLauncher;
    private final Job driverPayoutJob;

    @Scheduled(fixedRate = 15000)
    public void runJob() {
        try {

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(Objects.requireNonNull(driverPayoutJob), jobParameters);

            log.info("Job finished successfully");
            System.out.println("BATCH JOB TRIGGERED: Checking for unpaid deliveries...");
        } catch (Exception e) {
            log.warn("Job finished with an exception: ", e);
        }
    }
}
