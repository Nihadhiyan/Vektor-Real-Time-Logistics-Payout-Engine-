package com.vektor.dispatch_engine.repository;

import com.vektor.dispatch_engine.model.DriverPayout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DriverPayoutRepository extends JpaRepository<DriverPayout, UUID> {
    Page<DriverPayout> findByDriverIdOrderByPayoutCalculatedAtDesc(String driverId, Pageable pageable);
}
