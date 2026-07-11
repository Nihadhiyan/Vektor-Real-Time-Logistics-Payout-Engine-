package com.vektor.dispatch_engine.exception;

import org.springframework.http.HttpStatus;
import com.vektor.dispatch_engine.exception.enums.ErrorCode;

public abstract class VektorTechnicalException extends VektorBaseException {

    protected VektorTechnicalException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(errorCode, httpStatus, message);
    }

    protected VektorTechnicalException(ErrorCode errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(errorCode, httpStatus, message, cause);
    }

}
