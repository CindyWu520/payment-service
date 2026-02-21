package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    private final WebhookRepository webhookRepository;

    private final WebhookReceivingService webhookReceivingService;

    public WebhookController(
            WebhookRepository webhookRepository,
            WebhookReceivingService webhookReceivingService) {
        this.webhookRepository = webhookRepository;
        this.webhookReceivingService = webhookReceivingService;
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
                                                 HttpServletRequest request) throws JsonProcessingException {
        // TODO: Verify the signature to confirm the webhook is from a trusted source

        // Respond immediately
        ResponseEntity<String> response = ResponseEntity.ok("Webhook sent successfully");

        // Offload heavy work to background
        webhookReceivingService.processWebhookAsync(payload, request.getRequestURL().toString());

        return response;
    }
}