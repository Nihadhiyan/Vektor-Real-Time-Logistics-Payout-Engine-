package com.vektor.dispatch_engine.repository;

import com.vektor.dispatch_engine.model.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID>
{
    // write custom methods
}
