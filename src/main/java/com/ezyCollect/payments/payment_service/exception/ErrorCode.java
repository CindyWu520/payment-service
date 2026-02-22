package com.ezyCollect.payments.payment_service.exception;

public enum ErrorCode {
    INVALID_REQUEST,
    PAYMENT_DECLINED,
    INSUFFICIENT_FUNDS,
    GATEWAY_TIMEOUT,
    WEBHOOK_ALREADY_EXISTS,
    DATABASE_ERROR,
    INTERNAL_ERROR
}
