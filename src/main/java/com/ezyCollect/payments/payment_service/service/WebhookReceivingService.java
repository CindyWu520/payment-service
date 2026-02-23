package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class WebhookReceivingService {
    private final ObjectMapper objectMapper;
    private final WebhookLogRepository webhookLogRepository;
    public WebhookReceivingService(WebhookLogRepository webhookLogRepository,
                                   ObjectMapper objectMapper) {
        this.webhookLogRepository = webhookLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void processWebhookAsync(PaymentResponse payload, String url) {
        // TODO: Process the business operations triggered by an incoming webhook event

        try {
            WebhookLog webhookLog = WebhookLog.builder()
                    .url(url)
                    .direction(WebhookDirection.INCOMING)
                    .payload(objectMapper.writeValueAsString(payload))
                    .eventStatus(WebhookEventStatus.RECEIVED)
                    .httpStatus(HttpStatus.OK.value())
                    .receiveAt(LocalDateTime.now())
                    .responseBody("Webhook received successfully")
                    .build();

            webhookLogRepository.save(webhookLog);
            log.info("Business operations are processed successfully for payment: {}", payload.transactionId());
        } catch (Exception e) {
            log.error("Failed the business operations for payment: {}", payload.transactionId());
        }
    }
}
