package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
public class WebhookService {
    private final ObjectMapper objectMapper;
    private final WebhookRepository webhookRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final RestTemplate restTemplate;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookLogRepository webhookLogRepository,
                          ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Async
    public void triggerWebhooks(PaymentResponse paymentResponse) {
        for (Webhook webhook : webhookRepository.findAllByActiveTrue()) {
            sendWebhook(webhook, paymentResponse);
        }
    }

    @Retryable(
        retryFor  = { WebhookException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    @Transactional
    public void sendWebhook(Webhook webhook, PaymentResponse paymentResponse) {
        WebhookLog webhookLog = createWebhookLog(webhook, paymentResponse);

        try{
            ResponseEntity<String> response = executeWebhookCall(webhook, paymentResponse);

            if (response.getStatusCode().is2xxSuccessful()) {
                handleSuccess(webhookLog, response);
            } else {
                handleFailure(webhookLog, new WebhookException(
                        "Non-2xx response: " + response.getStatusCode()));
            }
        } catch (Exception e) {
            handleFailure(webhookLog, e);
            throw new WebhookException("Webhook failed due to network error: " + e.getMessage(), e);
        }
    }

    private void handleFailure(WebhookLog webhookLog, Exception ex) {
        webhookLog.setEventStatus(WebhookEventStatus.FAILED);
        webhookLog.setRetryCount(webhookLog.getRetryCount() + 1);
        webhookLog.setHttpStatus(null);
        webhookLog.setResponseBody(ex.getMessage());
        webhookLogRepository.save(webhookLog);
    }

    private void handleSuccess(WebhookLog webhookLog, ResponseEntity<String> response) {
        webhookLog.setHttpStatus(response.getStatusCode().value());
        webhookLog.setResponseBody(response.getBody());

        webhookLogRepository.save(webhookLog);
    }

    private ResponseEntity<String> executeWebhookCall(Webhook webhook,
                                                      PaymentResponse paymentResponse) {
        return restTemplate.postForEntity(
                webhook.getUrl(),
                paymentResponse,
                String.class
        );
    }

    private WebhookLog createWebhookLog(Webhook webhook, PaymentResponse paymentResponse) {
        String payload = convertToJson(paymentResponse);
        WebhookLog webhookLog = WebhookLog.builder()
                .webhookId(webhook.getId())
                .direction(WebhookDirection.OUTGOING)
                .url(webhook.getUrl())
                .payload(payload)
                .eventStatus(WebhookEventStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .retryCount(0).build();

        return webhookLogRepository.save(webhookLog);
    }

    private String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
    @Recover
    public void recover(Exception e, String url, PaymentResponse paymentResponse) {
        System.out.println("Webhook permanently failed: " + url + ", reason: " + e.getMessage());
    }
}
