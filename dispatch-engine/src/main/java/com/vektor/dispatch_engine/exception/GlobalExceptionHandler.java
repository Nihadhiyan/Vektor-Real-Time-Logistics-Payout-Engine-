package com.vektor.dispatch_engine.exception;

import com.vektor.dispatch_engine.exception.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(VektorBaseException.class)
    public ResponseEntity<ProblemDetail> handleVektorBaseException(VektorBaseException ex) {
        if (ex instanceof VektorBusinessException) {
            log.warn("Business rule violation [{}]: {}", ex.getErrorCode(), ex.getMessage());
        } else {
            log.error("Technical/System exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        }
        
        HttpStatus status = ex.getHttpStatus();
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/" + (ex.getErrorCode() != null ? ex.getErrorCode().name().toLowerCase().replace("_", "-") : "unknown"))));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", status.value());
        problemDetail.setProperty("error", ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.SYSTEM_ERROR);
        
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request payload. Please check the 'errors' property for details.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/validation-failed")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.BAD_REQUEST.value());
        problemDetail.setProperty("error", ErrorCode.VALIDATION_ERROR);
        problemDetail.setProperty("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            violations.put(propertyPath, violation.getMessage());
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request payload. Please check the 'errors' property for details.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/constraint-violation")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.BAD_REQUEST.value());
        problemDetail.setProperty("error", ErrorCode.CONSTRAINT_VIOLATION);
        problemDetail.setProperty("errors", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Database constraint/integrity violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A data conflict or duplicate entry occurred. Please verify your data and try again.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/data-conflict")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.CONFLICT.value());
        problemDetail.setProperty("error", ErrorCode.DUPLICATE_DELIVERY_EVENT);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON payload or unreadable HTTP request body: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request or unreadable request body. Please check your JSON syntax and data types.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/malformed-payload")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.BAD_REQUEST.value());
        problemDetail.setProperty("error", ErrorCode.VALIDATION_ERROR);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingPathVariableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleRequestParameterExceptions(Exception ex) {
        log.warn("Invalid or missing request parameter: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing or invalid request parameter: " + ex.getMessage());
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/invalid-parameter")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.BAD_REQUEST.value());
        problemDetail.setProperty("error", ErrorCode.VALIDATION_ERROR);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not allowed: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/method-not-allowed")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        problemDetail.setProperty("error", ErrorCode.UNSUPPORTED_OPERATION);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problemDetail);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type '" + ex.getContentType() + "' is not supported.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/unsupported-media-type")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        problemDetail.setProperty("error", ErrorCode.UNSUPPORTED_OPERATION);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ProblemDetail> handleNotFoundException(Exception ex) {
        log.warn("Requested resource or endpoint not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested API endpoint or resource does not exist.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/resource-not-found")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.NOT_FOUND.value());
        problemDetail.setProperty("error", ErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        log.error("Unhandled unexpected exception caught by global handler:", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred while processing your request.");
        problemDetail.setType(Objects.requireNonNull(URI.create("https://vektor.com/errors/internal-server-error")));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetail.setProperty("error", ErrorCode.SYSTEM_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    
}
