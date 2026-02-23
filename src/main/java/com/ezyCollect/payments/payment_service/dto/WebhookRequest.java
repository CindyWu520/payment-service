package com.ezyCollect.payments.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

@Builder
public record WebhookRequest (
        @URL(message = "Must be a valid URL")
        @NotBlank(message = "Webhook URL cannot be blank")
        @Size(max = 255, message = "Webhook URL must not exceed 255 characters")
        String url
) {}
