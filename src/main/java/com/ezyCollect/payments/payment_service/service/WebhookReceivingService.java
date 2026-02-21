package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
public class WebhookReceivingService {
    private final ObjectMapper objectMapper;
    private final WebhookLogRepository webhookLogRepository;
    public WebhookReceivingService(WebhookLogRepository webhookLogRepository,
                                   ObjectMapper objectMapper) {
        this.webhookLogRepository = webhookLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Retryable(
        value = {WebhookException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2) // Exponential backoff: 2s, 4s, 8s
    )
    @Transactional
    public void processWebhookAsync(PaymentResponse payload, String url) {
        // TODO: Process the business operations triggered by an incoming webhook event

        try {
            WebhookLog webhookLog = WebhookLog.builder()
                    .url(url)
                    .direction(WebhookDirection.INCOMING)
                    .payload(objectMapper.writeValueAsString(payload))
                    .eventStatus(WebhookEventStatus.RECEIVED)
                    .httpStatus(200)
                    .receiveAt(LocalDateTime.now())
                    .responseBody("Webhook received successfully")
                    .retryCount(0)
                    .build();

            webhookLogRepository.save(webhookLog);
        } catch (Exception e) {
            throw new WebhookException("Failed to process webhook", e);
        }
    }
}
