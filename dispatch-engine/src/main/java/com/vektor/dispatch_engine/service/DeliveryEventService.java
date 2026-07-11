package com.vektor.dispatch_engine.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.vektor.dispatch_engine.dto.deliveryevent.mapper.DeliveryEventMapper;
import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.dto.deliveryevent.response.UnpaidDeliveryResponse;
import com.vektor.dispatch_engine.repository.DeliveryEventRepository;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventService {

    private final DeliveryEventRepository deliveryEventRepository;
    private final DeliveryEventMapper deliveryEventMapper;

    @Transactional(readOnly = true)
    public List<UnpaidDeliveryResponse> getUnpaidDeliveries(Pageable pageable) {

        log.info("API Request received: Fetching unpaid delivery queue");

        var entities = deliveryEventRepository.findByProcessedFalseOrderByReceivedAtAsc(pageable);

        var response = deliveryEventMapper.toUnpaidResponseList(entities.toList());

        return response;
    }

    @Transactional
    public UnpaidDeliveryResponse ingestEvent(DeliveryEventUpdateRequest request) {
        log.info("API Request received: Ingesting delivery event {} for driver {}", request.eventId(), request.driverId());
        var entity = deliveryEventMapper.toDeliveryEvent(request);
        var saved = deliveryEventRepository.save(Objects.requireNonNull(entity));
        return deliveryEventMapper.toUnpaidResponse(saved);
    }

}
