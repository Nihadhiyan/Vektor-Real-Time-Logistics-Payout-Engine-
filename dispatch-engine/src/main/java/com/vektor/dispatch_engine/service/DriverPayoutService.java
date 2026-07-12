package com.vektor.dispatch_engine.service;

import java.time.Instant;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vektor.dispatch_engine.dto.payout.mapper.DriverPayoutMapper;
import com.vektor.dispatch_engine.dto.payout.request.DriverPayoutRequest;
import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.dto.payout.response.SettlementTriggerResponse;
import com.vektor.dispatch_engine.model.DriverPayout;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverPayoutService {
    private final DriverPayoutRepository driverPayoutRepository;
    private final DriverPayoutMapper driverPayoutMapper;
    private final JobLauncher jobLauncher;
    private final Job driverPayoutJob;

    @Transactional(readOnly = true)
    public List<DriverPayoutResponse> getDriverPayouts(String driverId, Pageable pageable) {
        log.info("API Request received: Fetching payout history for driver {}", driverId);

        var entities = driverPayoutRepository.findByDriverIdOrderByPayoutCalculatedAtDesc(driverId, pageable);

        var response = driverPayoutMapper.toDriverPayoutResponseList(entities.toList());
        return response;
    }

    public SettlementTriggerResponse triggerSettlement() {
        Instant cutoff = Instant.now().minusSeconds(5);
        try {
            log.info("API Request received: Triggering manual batch settlement job");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("cutoff", Objects.requireNonNull(cutoff.toString()))
                    .toJobParameters();

            var execution = jobLauncher.run(Objects.requireNonNull(driverPayoutJob), jobParameters);
            return new SettlementTriggerResponse(
                    execution.getStatus().toString(),
                    cutoff.toString(),
                    "Batch settlement triggered successfully. Execution Status: " + execution.getStatus()
            );
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("Settlement for this cutoff already completed: {}", e.getMessage());
            return new SettlementTriggerResponse(
                    "ALREADY_COMPLETED",
                    cutoff.toString(),
                    "Settlement already completed for this cutoff — nothing to do."
            );
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Settlement job already in progress");
            return new SettlementTriggerResponse(
                    "ALREADY_RUNNING",
                    cutoff.toString(),
                    "Settlement job is already running."
            );
        } catch (Exception e) {
            log.error("Failed to trigger manual batch settlement job", e);
            throw new RuntimeException("Failed to trigger batch settlement job: " + e.getMessage(), e);
        }
    }

    @Transactional
    public DriverPayoutResponse calculatePayout(DriverPayoutRequest request) {
        log.info("API Request received: Calculating payout for driver {} amount {}", request.driverId(), request.totalAmount());
        var payout = new DriverPayout(
                request.driverId(),
                request.totalAmount(),
                request.deliveriesProcessed()
        );
        var saved = driverPayoutRepository.save(payout);
        return driverPayoutMapper.toDriverPayoutResponse(saved);
    }

}
