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
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
public class WebhookService {
    private final ObjectMapper objectMapper;
    private final WebhookRepository webhookRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final RestTemplate restTemplate;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookLogRepository webhookLogRepository,
                          ObjectMapper objectMapper,
                          RestTemplate restTemplate) {
        this.webhookRepository = webhookRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    public void triggerWebhooks(PaymentResponse paymentResponse) {
        for (Webhook webhook : webhookRepository.findAllByActiveTrue()) {
            WebhookLog webhookLog = createWebhookLog(webhook, paymentResponse);
            webhookLogRepository.save(webhookLog);
            sendWebhook(webhook, webhookLog, paymentResponse);
        }
    }

    @Retryable(
        retryFor  = { WebhookException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendWebhook(Webhook webhook,
                            WebhookLog webhookLog,
                            PaymentResponse paymentResponse) {
        ResponseEntity<String> response;

        try {
            response = executeWebhookCall(webhook, paymentResponse);
        } catch (ResourceAccessException ex) {
            // Internet / timeout error
            throw new WebhookException(
                    ErrorCode.INTERNAL_ERROR,
                    "Network error while calling webhook",
                    ex);
        } catch (RestClientException ex) {
            // Other HTTP client errors
            throw new WebhookException(
                    ErrorCode.INVALID_REQUEST,
                    "HTTP client error while calling webhook",
                    ex);
        }

        // Separate non-2xx response
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new WebhookException(
                    ErrorCode.INVALID_REQUEST,
                    "Non-2xx response: " + response.getStatusCode());
        }

        handleSuccess(webhookLog, response);
    }

    @Transactional
    public void handleFailure(WebhookLog webhookLog, Exception ex) {

        webhookLog.setEventStatus(WebhookEventStatus.FAILED);
        webhookLog.setHttpStatus(null);
        webhookLog.setResponseBody(ex.getMessage());
        webhookLog.setSentAt(LocalDateTime.now());

        webhookLogRepository.save(webhookLog);
    }

    @Transactional
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
            throw new WebhookException(
                    ErrorCode.INVALID_REQUEST,
                    "Failed to serialize payload",
                    e);
        }
    }
    @Recover
    public void recover(WebhookException ex,
                        Webhook webhook,
                        WebhookLog webhookLog,
                        PaymentResponse paymentResponse) {
        handleFailure(webhookLog, ex);
        log.warn(
                "Webhook permanently failed after retries. webhookId={}, transactionId={}, reason={}",
                webhook.getId(),
                paymentResponse != null ? paymentResponse.getTransactionId() : "unknown",
                ex.getMessage(),
                ex
        );
    }

    public Webhook registerWebhook(WebhookRequest request) {
        try {
            Webhook webhook = Webhook.builder()
                    .url(request.getUrl())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return webhookRepository.save(webhook);
        } catch (Exception e) {
            // Wrap DB exceptions in a custom runtime exception
            throw new WebhookException(
                    ErrorCode.DATABASE_ERROR,
                    "Failed to save webhook to database",
                    e
            );
        }
    }
}
