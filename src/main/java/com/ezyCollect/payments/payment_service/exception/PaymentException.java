package com.ezyCollect.payments.payment_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException{
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(
            ErrorCode errorCode,
            String message,
            HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PaymentException(
            ErrorCode errorCode,
            HttpStatus httpStatus,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
