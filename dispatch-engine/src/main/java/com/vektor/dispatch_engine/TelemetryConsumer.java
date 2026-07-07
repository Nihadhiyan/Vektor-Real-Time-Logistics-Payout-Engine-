package com.vektor.dispatch_engine;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TelemetryConsumer {

    @KafkaListener(topics = "delivery-updates", groupId = "dispatch-processor-group")
    public void consumeTelemetry(DeliveryUpdate update) {
        System.out.println("VEKTOR DISPATCH: Driver " + update.driverId() +
                            " is " + update.status() +
                            " at [" + update.lat() + "," + update.lng() + "]");
    }

}
