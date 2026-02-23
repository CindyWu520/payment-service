package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.GlobalExceptionHandler;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.ezyCollect.payments.payment_service.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {WebhookController.class, GlobalExceptionHandler.class})
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookService webhookService;

    @MockBean
    private WebhookReceivingService webhookReceivingService;

    @MockBean
    private WebhookRepository webhookRepository;

    private WebhookRequest validWebhookRequest;
    private PaymentResponse paymentResponse;
    private Webhook savedWebhook;

    private static final String REGISTER_URL = "/v1/webhooks/register";
    private static final String RECEIVE_URL  = "/v1/webhooks/receive";

    @BeforeEach
    void setUp() {
        validWebhookRequest = new WebhookRequest("http://example.com/webhook");

        paymentResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();

        savedWebhook = Webhook.builder()
                .id(1L)
                .url("http://example.com/webhook")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── POST /register — Happy Path ──────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 CREATED when webhook is registered successfully")
    void registerWebhook_success_returns201() throws Exception {
        // Arrange
        when(webhookService.registerWebhook(any(WebhookRequest.class)))
                .thenReturn(savedWebhook);

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validWebhookRequest)))
                .andExpect(status().isCreated());

        verify(webhookService).registerWebhook(any(WebhookRequest.class));
    }

    @Test
    @DisplayName("Should return Webhook register successfully on successful registration")
    void registerWebhook_success_returnsEmptyBody() throws Exception {
        // Arrange
        when(webhookService.registerWebhook(any())).thenReturn(savedWebhook);

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validWebhookRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Webhook register successfully"));
    }

    // ─── POST /register — Validation Errors ──────────────────────────────────

    @Test
    @DisplayName("Should return 400 when webhook URL is blank")
    void registerWebhook_blankUrl_returns400() throws Exception {
        // Arrange
        WebhookRequest blankRequest = new WebhookRequest("");

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.url").exists());

        verify(webhookService, never()).registerWebhook(any());
    }

    @Test
    @DisplayName("Should return 400 when webhook URL is not a valid URL format")
    void registerWebhook_invalidUrlFormat_returns400() throws Exception {
        // Arrange
        WebhookRequest invalidRequest = new WebhookRequest("not-a-valid-url");

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.url").exists());

        verify(webhookService, never()).registerWebhook(any());
    }

    @Test
    @DisplayName("Should return 400 when request body is missing")
    void registerWebhook_missingBody_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_BODY_MISSING"));

        verify(webhookService, never()).registerWebhook(any());
    }

    // ─── POST /register — Service Exceptions ─────────────────────────────────

    @Test
    @DisplayName("Should return 409 when webhook URL already exists")
    void registerWebhook_duplicateUrl_returns409() throws Exception {
        // Arrange
        when(webhookService.registerWebhook(any()))
                .thenThrow(new WebhookException(ErrorCode.WEBHOOK_ALREADY_EXISTS));

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validWebhookRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WEBHOOK_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value(REGISTER_URL));
    }

    @Test
    @DisplayName("Should return 500 when webhook registration DB save fails")
    void registerWebhook_dbSaveFails_returns500() throws Exception {
        // Arrange
        when(webhookService.registerWebhook(any()))
                .thenThrow(new WebhookException(ErrorCode.WEBHOOK_REGISTER_FAILED));

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validWebhookRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("WEBHOOK_REGISTER_FAILED"));
    }

    // ─── POST /receive — Happy Path ───────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 OK with success message when webhook is received")
    void receiveWebhook_success_returns200() throws Exception {
        // Arrange
        doNothing().when(webhookReceivingService)
                .processWebhookAsync(any(PaymentResponse.class), anyString());

        // Act & Assert
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook sent successfully"));
    }

    @Test
    @DisplayName("Should call processWebhookAsync with correct payload and URL")
    void receiveWebhook_success_callsProcessWebhookAsync() throws Exception {
        // Arrange
        doNothing().when(webhookReceivingService)
                .processWebhookAsync(any(PaymentResponse.class), anyString());

        // Act
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk());

        // Assert — processWebhookAsync was called once
        verify(webhookReceivingService).processWebhookAsync(
                argThat(p -> p.transactionId().equals("42") &&
                        p.status().equals("SUCCESS")),
                anyString()
        );
    }

    @Test
    @DisplayName("Should return 200 immediately — fire and forget, not blocked by async processing")
    void receiveWebhook_fireAndForget_respondsImmediately() throws Exception {
        // Arrange — simulate slow async processing
        doNothing().when(webhookReceivingService)
                .processWebhookAsync(any(), anyString());

        // Act & Assert — controller returns 200 without waiting for async
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook sent successfully"));
    }

    @Test
    @DisplayName("Should accept request without X-Signature header")
    void receiveWebhook_noSignatureHeader_returns200() throws Exception {
        // Arrange
        doNothing().when(webhookReceivingService)
                .processWebhookAsync(any(), anyString());

        // Act & Assert — signature is optional (required = false)
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should accept request with X-Signature header")
    void receiveWebhook_withSignatureHeader_returns200() throws Exception {
        // Arrange
        doNothing().when(webhookReceivingService)
                .processWebhookAsync(any(), anyString());

        // Act & Assert
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", "sha256=abc123")
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 200 even when processWebhookAsync throws — fire and forget")
    void receiveWebhook_asyncThrows_stillReturns200() throws Exception {
        // Arrange — async method throws but should not affect response
        doThrow(new RuntimeException("Async processing failed"))
                .when(webhookReceivingService)
                .processWebhookAsync(any(), anyString());

        // Act & Assert — controller already responded before async runs
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentResponse)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook sent successfully"));
    }

    // ─── POST /receive — Validation ───────────────────────────────────────────

    @Test
    @DisplayName("Should return 400 when receive request body is missing")
    void receiveWebhook_missingBody_returns400() throws Exception {
        mockMvc.perform(post(RECEIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(webhookReceivingService, never()).processWebhookAsync(any(), anyString());
    }
}