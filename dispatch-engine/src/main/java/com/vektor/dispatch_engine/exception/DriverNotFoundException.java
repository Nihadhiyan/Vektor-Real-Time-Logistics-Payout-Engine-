package com.vektor.dispatch_engine.exception;

import org.springframework.http.HttpStatus;

import com.vektor.dispatch_engine.exception.enums.ErrorCode;

public class DriverNotFoundException extends VektorBusinessException {

    public DriverNotFoundException(String driverId, Throwable cause) {
        super(ErrorCode.DRIVER_NOT_FOUND, HttpStatus.NOT_FOUND, "Driver with ID " + driverId + " not found",
                cause);
    }

    public DriverNotFoundException(String driverId) {
        super(ErrorCode.DRIVER_NOT_FOUND, HttpStatus.NOT_FOUND, "Driver with ID " + driverId + " not found");
    }
}
