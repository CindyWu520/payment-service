package com.ezyCollect.payments.payment_service.exception;

public class WebhookException extends RuntimeException {
    private final String errorCode;

    public WebhookException(String message) {
        super(message);
        this.errorCode = "WEBHOOK_FAILED";
    }

    public WebhookException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEBHOOK_FAILED";
    }

    public WebhookException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebhookException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
