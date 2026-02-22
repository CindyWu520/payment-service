package com.ezyCollect.payments.payment_service.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WebhookResponse(
        Long id,
        String url,
        boolean active,
        LocalDateTime createdAt
) {}
