package com.ezyCollect.payments.payment_service.exception;

public class EncryptionException extends RuntimeException{
    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
