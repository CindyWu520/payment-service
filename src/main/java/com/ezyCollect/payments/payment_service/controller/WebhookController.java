package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.dto.WebhookResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.ezyCollect.payments.payment_service.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    private final WebhookReceivingService webhookReceivingService;

    private final WebhookService webhookService;

    public WebhookController(
            WebhookRepository webhookRepository,
            WebhookReceivingService webhookReceivingService,
            WebhookService webhookService) {
        this.webhookReceivingService = webhookReceivingService;
        this.webhookService = webhookService;
    }

    @PostMapping("/register")
    public ResponseEntity<WebhookResponse> registerWebhook(@RequestBody @Valid WebhookRequest request) {
        Webhook savedWebhook;
        try {
            savedWebhook = webhookService.registerWebhook(request);
        } catch (WebhookException e) {
            WebhookResponse response = WebhookResponse.builder()
                    .url(request.getUrl())
                    .active(false)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        WebhookResponse response = WebhookResponse.builder()
                .id(savedWebhook.getId())
                .url(savedWebhook.getUrl())
                .active(savedWebhook.isActive())
                .createdAt(savedWebhook.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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