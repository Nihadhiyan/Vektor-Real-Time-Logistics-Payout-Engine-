package com.vektor.dispatch_engine.consumer;

import com.vektor.dispatch_engine.dto.deliveryevent.mapper.DeliveryEventMapper;
import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import org.slf4j.MDC;

import org.springframework.dao.DataIntegrityViolationException;
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

        String driverLabel = update.driverId() != null ? update.driverId() : "UNKNOWN_DRIVER";
        MDC.put("driverId", driverLabel);

        try {
            deliveryEventRepository.save(Objects.requireNonNull(deliveryEventMapper.toDeliveryEvent(update)));
            log.info("Persisted event: eventId={} status={}", update.eventId(), update.status());
        } catch (DataIntegrityViolationException e) {
            log.warn("DUPLICATE BLOCKED: Event {} was already processed. Ignoring.", update.eventId());
        } catch (Exception e) {
            log.error("Unexpected error processing event: ", e);
        } finally {
            MDC.clear();
        }

    }

}
