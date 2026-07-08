package com.vektor.dispatch_engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutJobScheduler {
    private final JobOperator jobOperator;
    private final Job driverPayoutJob;

    @Scheduled(fixedRate = 15000)
    public void runJob() {
        try {

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobOperator.start(driverPayoutJob, jobParameters);

            log.info("Job finished successfully");
            System.out.println("BATCH JOB TRIGGERED: Checking for unpaid deliveries...");
        } catch (Exception e) {
            log.warn("Job finished with an exception: ", e);
        }
    }
}
