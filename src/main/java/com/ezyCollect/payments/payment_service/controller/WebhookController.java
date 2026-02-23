package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.ezyCollect.payments.payment_service.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    private final WebhookReceivingService webhookReceivingService;

    private final WebhookService webhookService;

    public WebhookController(
            WebhookReceivingService webhookReceivingService,
            WebhookService webhookService) {
        this.webhookReceivingService = webhookReceivingService;
        this.webhookService = webhookService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerWebhook(@RequestBody @Valid WebhookRequest request) {
        webhookService.registerWebhook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Webhook register successfully");
    }

    @PostMapping("/receive")
    public ResponseEntity<String> receiveWebhook(@RequestBody PaymentResponse payload,
                                                 @RequestHeader(value = "X-Signature", required = false) String signature,
                                                 HttpServletRequest request) throws JsonProcessingException {
        // TODO: Verify the signature to confirm the webhook is from a trusted source

        // fire and forget : Offload heavy work to background
        try{
            webhookReceivingService.processWebhookAsync(payload, request.getRequestURL().toString());
        } catch (Exception e) {
            log.warn("Async webhook processing failed for transactionI={}: {}",
                    payload.transactionId(), e.getMessage());
        }

        // Respond immediately
        return ResponseEntity.ok("Webhook sent successfully");
    }
}