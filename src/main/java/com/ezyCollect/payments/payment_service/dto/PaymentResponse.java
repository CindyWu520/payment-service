package com.ezyCollect.payments.payment_service.dto;

import lombok.Builder;


@Builder
public record PaymentResponse(
        String status,
        String transactionId
) {}

