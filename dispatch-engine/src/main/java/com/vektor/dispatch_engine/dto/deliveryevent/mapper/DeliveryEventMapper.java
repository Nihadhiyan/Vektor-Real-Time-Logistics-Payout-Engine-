package com.vektor.dispatch_engine.dto.deliveryevent.mapper;

import com.vektor.dispatch_engine.dto.deliveryevent.request.DeliveryEventUpdateRequest;
import com.vektor.dispatch_engine.model.DeliveryEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = Instant.class)
public interface DeliveryEventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receivedAt", expression = "java(Instant.now())")
    @Mapping(target = "processed", constant = "false")
    DeliveryEvent toDeliveryEvent(DeliveryEventUpdateRequest deliveryEventUpdateRequest);
}
