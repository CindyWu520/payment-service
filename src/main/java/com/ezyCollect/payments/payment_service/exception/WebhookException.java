package com.ezyCollect.payments.payment_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WebhookException extends RuntimeException {
    private final ErrorCode errorCode;
    private HttpStatus httpStatus;

    public WebhookException(
            ErrorCode errorCode,
            HttpStatus httpStatus,
            String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public WebhookException(
            ErrorCode errorCode,
            String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebhookException(
            ErrorCode errorCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
