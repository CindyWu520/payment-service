package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.PaymentServiceApplication;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PaymentServiceApplication.class)
@AutoConfigureMockMvc
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookRepository webhookRepository;

    @MockBean
    private WebhookReceivingService webhookReceivingService;

    @Test
    void registerWebhook_success() throws Exception {
        // Arrange
        WebhookRequest request = new WebhookRequest();
        request.setUrl("https://example.com/webhook");

        Webhook savedWebhook = Webhook.builder()
                .id(1L)
                .url(request.getUrl())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(webhookRepository.save(any(Webhook.class))).thenReturn(savedWebhook);

        // Act & Assert
        mockMvc.perform(post("/v1/webhooks/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.url").value("https://example.com/webhook"))
                .andExpect(jsonPath("$.active").value(true));

        verify(webhookRepository, times(1)).save(any(Webhook.class));
    }

    @Test
    void receiveWebhook_success() throws Exception {
        // Arrange
        PaymentResponse payload = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .errorMessage(null)
                .build();

        // Act & Assert
        mockMvc.perform(post("/v1/webhooks/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("X-Signature", "dummy-signature"))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook sent successfully"));

        // Verify async service was called
        verify(webhookReceivingService, times(1))
                .processWebhookAsync(any(PaymentResponse.class), anyString());
    }
}