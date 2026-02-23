package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {
    private final ObjectMapper objectMapper;
    private final WebhookRepository webhookRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final WebhookSenderService webhookSenderService;

    @Async
    public void triggerWebhooks(PaymentResponse paymentResponse) {
        for (Webhook webhook : webhookRepository.findAllByActiveTrue()) {
            WebhookLog webhookLog = createWebhookLog(webhook, paymentResponse);
            webhookLogRepository.save(webhookLog);
            webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse);
        }
    }

    private WebhookLog createWebhookLog(Webhook webhook, PaymentResponse paymentResponse) {
        String payload = convertToJson(paymentResponse);
         return WebhookLog.builder()
                .webhookId(webhook.getId())
                .direction(WebhookDirection.OUTGOING)
                .url(webhook.getUrl())
                .payload(payload)
                .eventStatus(WebhookEventStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new WebhookException(ErrorCode.WEBHOOK_PAYLOAD_SERIALIZATION_FAILED, e);
        }
    }

    public Webhook registerWebhook(WebhookRequest request) {

        if (webhookRepository.existsByUrl(request.url())) {
            throw new WebhookException(ErrorCode.WEBHOOK_ALREADY_EXISTS);
        }

        try {
            Webhook webhook = Webhook.builder()
                    .url(request.url())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return webhookRepository.save(webhook);
        } catch (Exception e) {
            // Wrap DB exceptions in a custom webhook exception
            throw new WebhookException(ErrorCode.WEBHOOK_REGISTER_FAILED, e);
        }
    }
}
