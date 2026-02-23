package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WebhookLogService {
    private final RestTemplate restTemplate;
    private final WebhookLogRepository webhookLogRepository;

    public ResponseEntity<String> executeWebhookCall(Webhook webhook,
                                                     PaymentResponse paymentResponse) {
        return restTemplate.postForEntity(
                webhook.getUrl(),
                paymentResponse,
                String.class
        );
    }

    @Transactional
    public void handleSuccess(WebhookLog webhookLog, ResponseEntity<String> response) {
        webhookLog.setHttpStatus(response.getStatusCode().value());
        webhookLog.setResponseBody(response.getBody());

        webhookLogRepository.save(webhookLog);
    }

    @Transactional
    public void handleFailure(WebhookLog webhookLog, Exception ex) {

        webhookLog.setEventStatus(WebhookEventStatus.FAILED);
        webhookLog.setHttpStatus(null);
        webhookLog.setResponseBody(ex.getMessage());

        webhookLogRepository.save(webhookLog);
    }
}
