package com.vektor.dispatch_engine.dto.payout.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.vektor.dispatch_engine.dto.payout.response.DriverPayoutResponse;
import com.vektor.dispatch_engine.model.DriverPayout;

@Mapper(componentModel = "spring")
public interface DriverPayoutMapper {
    DriverPayoutResponse toDriverPayoutResponse(DriverPayout entity);

    List<DriverPayoutResponse> toDriverPayoutResponseList(List<DriverPayout> entities);
}
