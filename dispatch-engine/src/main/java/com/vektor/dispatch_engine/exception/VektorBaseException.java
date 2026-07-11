package com.vektor.dispatch_engine.exception;

import org.springframework.http.HttpStatus;
import com.vektor.dispatch_engine.exception.enums.ErrorCode;
import lombok.Getter;

@Getter
public abstract class VektorBaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    protected VektorBaseException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected VektorBaseException(ErrorCode errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
