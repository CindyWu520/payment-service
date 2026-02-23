package com.ezyCollect.payments.payment_service.exception;

import lombok.Getter;

@Getter
public class WebhookException extends RuntimeException {
    private final ErrorCode errorCode;

    public WebhookException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public WebhookException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
