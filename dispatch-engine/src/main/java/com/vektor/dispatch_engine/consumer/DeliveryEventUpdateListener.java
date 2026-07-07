package com.vektor.dispatch_engine.consumer;

import com.vektor.dispatch_engine.dto.deliveryevent.mapper.DeliveryEventMapper;
import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryEventUpdateListener {

    private final DeliveryEventRepository deliveryEventRepository;
    private final DeliveryEventMapper deliveryEventMapper;

    @KafkaListener(topics = "delivery-updates", groupId = "dispatch-processor-group")
    public void onDeliveryEventUpdate(DeliveryEventUpdateRequest update) {
        deliveryEventRepository.save(deliveryEventMapper.toDeliveryEvent(update));
        log.info("Persisted event: driver={} status={}", update.driverId(), update.status());
    }

}
