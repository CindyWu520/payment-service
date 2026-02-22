package com.ezyCollect.payments.payment_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
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

        log.error("Payment error occurred: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .error(ex.getHttpStatus().getReasonPhrase())
                .status(ex.getHttpStatus().value())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    // ==============================
    //  WebhookException
    // ==============================
    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<ErrorResponse> handleWebhookException(
            WebhookException ex,
            HttpServletRequest request) {

        log.error("Webhook error occurred: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message("Webhook delivery failed")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // ==============================
    //  Validation Errors (@Valid)
    // ==============================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", validationErrors);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST)
                .message(validationErrors)
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // ==============================
    //  Catch-All (System Errors)
    // ==============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected system error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR)
                .message("Something went wrong. Please contact support.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
