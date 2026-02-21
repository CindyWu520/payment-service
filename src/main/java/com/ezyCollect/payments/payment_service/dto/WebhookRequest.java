package com.ezyCollect.payments.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRequest {
    @NotNull(message = "ID cannot be null")
    private Long id;

    @NotBlank(message = "Webhook URL cannot be blank")
    @Size(max = 255, message = "Webhook URL must not exceed 255 characters")
    private String url;
}
