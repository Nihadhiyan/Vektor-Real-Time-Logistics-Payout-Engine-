package com.vektor.dispatch_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vektor.dispatch_engine.model.DriverRateCards;

@Repository
public interface DriverRateCardRepository extends JpaRepository<DriverRateCards, String> {

    DriverRateCards findByDriverId(String driverId);
}
