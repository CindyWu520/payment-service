package com.ezyCollect.payments.payment_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==============================
    //  PaymentException
    // ==============================
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex,
                                                                HttpServletRequest request) {

        log.error("Payment error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.
                status(ex.getErrorCode().getHttpStatus()).
                body(buildResponse(ex.getErrorCode(), request.getRequestURI()));
    }

    // ==============================
    //  WebhookException
    // ==============================
    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<ErrorResponse> handleWebhookException(
            WebhookException ex,
            HttpServletRequest request) {

        log.error("Payment error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(buildResponse(ex.getErrorCode(), request.getRequestURI()));
    }

    // ==============================
    //  Validation Errors (@Valid)
    // ==============================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                        (existing, duplicate) -> existing
                ));

        log.warn("Validation failed: {}", validationErrors);

        return ResponseEntity.badRequest().body(buildResponse(ErrorCode.VALIDATION_ERROR, request.getRequestURI(), validationErrors));
    }

    // ==============================
    //  Catch-All (System Errors)
    // ==============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected system error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed or missing request body: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(ErrorCode.REQUEST_BODY_MISSING, request.getRequestURI()));
    }

    private ErrorResponse buildResponse(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .build();
    }

    private ErrorResponse buildResponse(ErrorCode errorCode, String path, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}
