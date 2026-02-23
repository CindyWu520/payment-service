package com.ezyCollect.payments.payment_service.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Payment errors
    CARD_DECLINED           ("CARD_DECLINED",            "Payment declined by issuing bank",          HttpStatus.UNPROCESSABLE_ENTITY),
    CARD_EXPIRED            ("CARD_EXPIRED",             "The card has expired",                      HttpStatus.UNPROCESSABLE_ENTITY),
    CARD_ENCRYPTION_ERROR   ("CARD_ENCRYPTION_ERROR",    "The card encryption failed",                HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_DECLINED        ("PAYMENT_DECLINED",         "A payment was declined by payment gateway", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_CARD_NUMBER     ("INVALID_CARD_NUMBER",      "The card number provided is invalid",       HttpStatus.BAD_REQUEST),
    DUPLICATE_PAYMENT       ("DUPLICATE_PAYMENT",        "A payment with this reference already exists", HttpStatus.CONFLICT),
    VALIDATION_ERROR        ("VALIDATION_ERROR",         "Validation failed",                         HttpStatus.BAD_REQUEST),
    REQUEST_BODY_MISSING    ("REQUEST_BODY_MISSING",     "Request body is missing or malformed",      HttpStatus.BAD_REQUEST),

    // Webhook errors
    WEBHOOK_NOT_FOUND        ("WEBHOOK_NOT_FOUND",        "No webhook registered for this event",      HttpStatus.NOT_FOUND),
    WEBHOOK_DELIVERY_FAILED  ("WEBHOOK_DELIVERY_FAILED",  "Failed to deliver webhook after retries",   HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_RECEIVING_FAILED ("WEBHOOK_RECEIVING_FAILED", "Failed to receive webhook after retries",   HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_SENDING_FAILED   ("WEBHOOK_SENDING_FAILED",   "Failed to send webhook after retries",      HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_ALREADY_EXISTS   ("WEBHOOK_ALREADY_EXISTS",   "Webhook with this url already exists",      HttpStatus.CONFLICT),
    WEBHOOK_ACCESS_FAILED    ("WEBHOOK_ACCESS_FAILED",    "Failed to access the webhook after retires",HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_CLIENT_ERROR     ("WEBHOOK_CLIENT_ERROR",     "Webhook client declined after reties",      HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_PAYLOAD_SERIALIZATION_FAILED     ("WEBHOOK_PAYLOAD_SERIALIZATION_FAILED",     "Failed to convert the payload to json",      HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_REGISTER_FAILED("WEBHOOK_REGISTER_FAILED",   "Failed to register webhook",               HttpStatus.INTERNAL_SERVER_ERROR),

    // System errors
    DATABASE_ERROR          ("DATABASE_ERROR",           "A database error occurred",                 HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR   ("INTERNAL_SERVER_ERROR",    "An unexpected error occurred",              HttpStatus.INTERNAL_SERVER_ERROR),
    GATEWAY_TIMEOUT         ("GATEWAY_TIMEOUT",          "Payment Gateway error occurred",            HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
