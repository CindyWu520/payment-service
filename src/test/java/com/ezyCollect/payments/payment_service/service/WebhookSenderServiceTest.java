package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookSenderServiceTest {

    @Mock
    private WebhookLogService webhookLogService;

    @InjectMocks
    private WebhookSenderService webhookSenderService;

    private Webhook webhook;
    private WebhookLog webhookLog;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        webhook = Webhook.builder()
                .id(1L)
                .url("http://localhost:8080/v1/webhooks/receive")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        webhookLog = WebhookLog.builder()
                .webhookId(1L)
                .direction(WebhookDirection.OUTGOING)
                .url(webhook.getUrl())
                .eventStatus(WebhookEventStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .build();

        paymentResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();
    }

    // ─── sendWebhook — Happy Path ─────────────────────────────────────────────

    @Test
    @DisplayName("Should call handleSuccess when webhook call returns 2xx")
    void sendWebhook_success_callsHandleSuccess() {
        // Arrange
        ResponseEntity<String> response = ResponseEntity.ok("received");
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenReturn(response);

        // Act
        webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse);

        // Assert
        verify(webhookLogService).executeWebhookCall(webhook, paymentResponse);
        verify(webhookLogService).handleSuccess(webhookLog, response);
    }

    @Test
    @DisplayName("Should not call handleFailure when webhook call succeeds")
    void sendWebhook_success_neverCallsHandleFailure() {
        // Arrange
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenReturn(ResponseEntity.ok("received"));

        // Act
        webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse);

        // Assert
        verify(webhookLogService, never()).handleFailure(any(), any());
    }

    // ─── sendWebhook — Non-2xx Response ──────────────────────────────────────

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_SENDING_FAILED when response is 4xx")
    void sendWebhook_4xxResponse_throwsWebhookSendingFailed() {
        // Arrange
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad request"));

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_SENDING_FAILED);
        verify(webhookLogService, never()).handleSuccess(any(), any());
    }

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_SENDING_FAILED when response is 5xx")
    void sendWebhook_5xxResponse_throwsWebhookSendingFailed() {
        // Arrange
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_SENDING_FAILED);
        verify(webhookLogService, never()).handleSuccess(any(), any());
    }

    // ─── sendWebhook — Network / Client Errors ────────────────────────────────

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_ACCESS_FAILED on ResourceAccessException")
    void sendWebhook_resourceAccessException_throwsWebhookAccessFailed() {
        // Arrange
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_ACCESS_FAILED);
        verify(webhookLogService, never()).handleSuccess(any(), any());
    }

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_CLIENT_ERROR on RestClientException")
    void sendWebhook_restClientException_throwsWebhookClientError() {
        // Arrange
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenThrow(new RestClientException("HTTP client error") {});

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_CLIENT_ERROR);
        verify(webhookLogService, never()).handleSuccess(any(), any());
    }

    @Test
    @DisplayName("Should wrap ResourceAccessException as cause in WebhookException")
    void sendWebhook_resourceAccessException_preservesCause() {
        // Arrange
        ResourceAccessException cause = new ResourceAccessException("Timeout");
        when(webhookLogService.executeWebhookCall(webhook, paymentResponse))
                .thenThrow(cause);

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookSenderService.sendWebhook(webhook, webhookLog, paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // ─── recover ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should call handleFailure with correct webhookLog and exception")
    void recover_callsHandleFailure() {
        // Arrange
        WebhookException ex = new WebhookException(ErrorCode.WEBHOOK_SENDING_FAILED);

        // Act
        webhookSenderService.recover(ex, webhook, webhookLog, paymentResponse);

        // Assert
        verify(webhookLogService).handleFailure(webhookLog, ex);
    }

    @Test
    @DisplayName("Should not throw exception when recover is called")
    void recover_doesNotThrow() {
        // Arrange
        WebhookException ex = new WebhookException(ErrorCode.WEBHOOK_ACCESS_FAILED);

        // Act & Assert
        assertThatCode(() ->
                webhookSenderService.recover(ex, webhook, webhookLog, paymentResponse)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null paymentResponse in recover without throwing")
    void recover_nullPaymentResponse_doesNotThrow() {
        // Arrange
        WebhookException ex = new WebhookException(ErrorCode.WEBHOOK_SENDING_FAILED);

        // Act & Assert — transactionId logs as "unknown"
        assertThatCode(() ->
                webhookSenderService.recover(ex, webhook, webhookLog, null)
        ).doesNotThrowAnyException();

        verify(webhookLogService).handleFailure(webhookLog, ex);
    }

    @Test
    @DisplayName("Should never call handleSuccess during recover")
    void recover_neverCallsHandleSuccess() {
        // Arrange
        WebhookException ex = new WebhookException(ErrorCode.WEBHOOK_SENDING_FAILED);

        // Act
        webhookSenderService.recover(ex, webhook, webhookLog, paymentResponse);

        // Assert
        verify(webhookLogService, never()).handleSuccess(any(), any());
    }

    @Test
    @DisplayName("Should never call executeWebhookCall during recover")
    void recover_neverCallsExecuteWebhookCall() {
        // Arrange
        WebhookException ex = new WebhookException(ErrorCode.WEBHOOK_SENDING_FAILED);

        // Act
        webhookSenderService.recover(ex, webhook, webhookLog, paymentResponse);

        // Assert
        verify(webhookLogService, never()).executeWebhookCall(any(), any());
    }
}