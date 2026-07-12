package com.vektor.dispatch_engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutJobScheduler {
    private final JobLauncher jobLauncher;
    private final Job driverPayoutJob;

    @Scheduled(fixedDelayString = "${vektor.payout.schedule-ms:60000}")
    public void runJob() {
        try {

            Instant cutoff = Instant.now().minusSeconds(5);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("cutoff", Objects.requireNonNull(cutoff.toString()))
                    .toJobParameters();

            jobLauncher.run(Objects.requireNonNull(driverPayoutJob), jobParameters);

            log.info("Job finished successfully");
            System.out.println("BATCH JOB TRIGGERED: Checking for unpaid deliveries...");
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("Settlement for this cutoff already completed: {}", e.getMessage());
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Settlement job already in progress");
        } catch (Exception e) {
            log.warn("Job finished with an exception: ", e);
        }
    }
}
