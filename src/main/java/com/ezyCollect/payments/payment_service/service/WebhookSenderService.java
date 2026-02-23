package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookSenderService {

    private final WebhookLogService webhookLogService;

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
            response = webhookLogService.executeWebhookCall(webhook, paymentResponse);
        } catch (ResourceAccessException e) {
            // Internet / timeout error
            throw new WebhookException(ErrorCode.WEBHOOK_ACCESS_FAILED, e);
        } catch (RestClientException e) {
            // Other HTTP client errors
            throw new WebhookException(ErrorCode.WEBHOOK_CLIENT_ERROR, e);
        }

        // Separate non-2xx response
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new WebhookException(ErrorCode.WEBHOOK_SENDING_FAILED);
        }

        webhookLogService.handleSuccess(webhookLog, response);
    }

    @Recover
    public void recover(WebhookException ex,
                        Webhook webhook,
                        WebhookLog webhookLog,
                        PaymentResponse paymentResponse) {
        webhookLogService.handleFailure(webhookLog, ex);
        log.warn(
                "Webhook permanently failed after retries. webhookId={}, transactionId={}, reason={}",
                webhook.getId(),
                paymentResponse != null ? paymentResponse.transactionId() : "unknown",
                ex.getMessage(),
                ex
        );
    }
}
