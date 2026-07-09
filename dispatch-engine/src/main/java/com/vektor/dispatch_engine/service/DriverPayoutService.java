package com.vektor.dispatch_engine.service;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<DriverPayoutResponse> getDriverPayouts(String driverId, Pageable pageable) {
        log.info("API Request received: Fetching payout history for driver {}", driverId);

        var entities = driverPayoutRepository.findByDriverIdOrderByPayoutCalculatedAtDesc(driverId, pageable);

        var response = driverPayoutMapper.toDriverPayoutResponseList(entities.toList());
        return response;
    }

}
