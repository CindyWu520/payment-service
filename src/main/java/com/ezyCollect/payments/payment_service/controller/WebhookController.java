package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    private final WebhookRepository webhookRepository;
    private final WebhookLogRepository webhookLogRepository;

    public WebhookController(WebhookRepository webhookRepository,
                             WebhookLogRepository webhookLogRepository) {
        this.webhookRepository = webhookRepository;
        this.webhookLogRepository = webhookLogRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Webhook> registerWebhook(@RequestBody WebhookRequest request) {

        Webhook webhook = Webhook.builder()
                .url(request.getUrl())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Webhook saved = webhookRepository.save(webhook);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/receive")
    public ResponseEntity<String> receiveWebhook(@RequestBody PaymentResponse payload,
                                                 @RequestHeader(value = "X-Signature", required = false) String signature,
                                                 HttpServletRequest request) {

        WebhookLog webhookLog = WebhookLog.builder()
                .url(String.valueOf(request.getRequestURL()))
                .direction(WebhookDirection.INCOMING)
                .payload(new ObjectMapper().writeValueAsString(payload))
                .eventStatus(WebhookEventStatus.RECEIVED)
                .httpStatus(200)
                .receiveAt(LocalDateTime.now())
                .responseBody("Webhook received successfully")
                .retryCount(0).build();

        webhookLogRepository.save(webhookLog);

        // Optionally: verify signature here (recommended in production)

        return ResponseEntity.ok("Webhook sent successfully");
    }

    @GetMapping
    public ResponseEntity<List<Webhook>> listWebhooks() {
        return ResponseEntity.ok(webhookRepository.findAllByActiveTrue());
    }
}