package com.vektor.dispatch_engine.service;

import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vektor.dispatch_engine.dto.payout.mapper.DriverPayoutMapper;
import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.repository.DriverPayoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverPayoutService {
    private final DriverPayoutRepository driverPayoutRepository;
    private final DriverPayoutMapper driverPayoutMapper;
    private final JobOperator jobOperator;
    private final Job driverPayoutJob;

    @Transactional(readOnly = true)
    public List<DriverPayoutResponse> getDriverPayouts(String driverId, Pageable pageable) {
        log.info("API Request received: Fetching payout history for driver {}", driverId);

        var entities = driverPayoutRepository.findByDriverIdOrderByPayoutCalculatedAtDesc(driverId, pageable);

        var response = driverPayoutMapper.toDriverPayoutResponseList(entities.toList());
        return response;
    }

    public String triggerSettlement() {
        try {
            log.info("API Request received: Triggering manual batch settlement job");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            var execution = jobOperator.start(driverPayoutJob, jobParameters);
            return "Batch settlement triggered successfully. Execution Status: " + execution.getStatus();
        } catch (Exception e) {
            log.error("Failed to trigger manual batch settlement job", e);
            throw new RuntimeException("Failed to trigger batch settlement job: " + e.getMessage(), e);
        }
    }

}
