package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookLogServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private WebhookLogRepository webhookLogRepository;

    @InjectMocks
    private WebhookLogService webhookLogService;

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
                .url(webhook.getUrl())
                .build();

        paymentResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();
    }

    // ─── executeWebhookCall ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should call restTemplate with correct URL and payload and return response")
    void executeWebhookCall_success_returnsResponse() {
        // Arrange
        ResponseEntity<String> mockResponse = ResponseEntity.ok("received");
        when(restTemplate.postForEntity(
                eq(webhook.getUrl()),
                eq(paymentResponse),
                eq(String.class))
        ).thenReturn(mockResponse);

        // Act
        ResponseEntity<String> result = webhookLogService.executeWebhookCall(webhook, paymentResponse);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("received");
        verify(restTemplate).postForEntity(webhook.getUrl(), paymentResponse, String.class);
    }

    @Test
    @DisplayName("Should propagate ResourceAccessException when network fails")
    void executeWebhookCall_networkFails_propagatesException() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        // Act & Assert
        assertThatThrownBy(() ->
                webhookLogService.executeWebhookCall(webhook, paymentResponse)
        ).isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Connection timed out");
    }

    @Test
    @DisplayName("Should return 4xx response without throwing")
    void executeWebhookCall_4xxResponse_returnsWithoutThrowing() {
        // Arrange
        ResponseEntity<String> badRequest = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("bad request");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(badRequest);

        // Act
        ResponseEntity<String> result = webhookLogService.executeWebhookCall(webhook, paymentResponse);

        // Assert — executeWebhookCall just returns the response, doesn't validate status
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should return 5xx response without throwing")
    void executeWebhookCall_5xxResponse_returnsWithoutThrowing() {
        // Arrange
        ResponseEntity<String> serverError = ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("server error");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(serverError);

        // Act
        ResponseEntity<String> result = webhookLogService.executeWebhookCall(webhook, paymentResponse);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ─── handleSuccess ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update webhook log with HTTP status and response body")
    void handleSuccess_updatesLogWithHttpStatusAndBody() {
        // Arrange
        ResponseEntity<String> response = ResponseEntity.ok("OK");

        // Act
        webhookLogService.handleSuccess(webhookLog, response);

        // Assert
        assertThat(webhookLog.getHttpStatus()).isEqualTo(200);
        assertThat(webhookLog.getResponseBody()).isEqualTo("OK");
        verify(webhookLogRepository).save(webhookLog);
    }

    @Test
    @DisplayName("Should save webhook log exactly once on success")
    void handleSuccess_savesLogExactlyOnce() {
        // Arrange
        ResponseEntity<String> response = ResponseEntity.ok("OK");

        // Act
        webhookLogService.handleSuccess(webhookLog, response);

        // Assert
        verify(webhookLogRepository, times(1)).save(webhookLog);
    }

    @Test
    @DisplayName("Should handle null response body without throwing")
    void handleSuccess_nullResponseBody_savesLog() {
        // Arrange
        ResponseEntity<String> response = ResponseEntity.ok(null);

        // Act
        webhookLogService.handleSuccess(webhookLog, response);

        // Assert
        assertThat(webhookLog.getHttpStatus()).isEqualTo(200);
        assertThat(webhookLog.getResponseBody()).isNull();
        verify(webhookLogRepository).save(webhookLog);
    }

    @Test
    @DisplayName("Should correctly set HTTP status for non-200 success responses")
    void handleSuccess_201Response_setsCorrectHttpStatus() {
        // Arrange
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.CREATED).body("created");

        // Act
        webhookLogService.handleSuccess(webhookLog, response);

        // Assert
        assertThat(webhookLog.getHttpStatus()).isEqualTo(201);
        verify(webhookLogRepository).save(webhookLog);
    }

    // ─── handleFailure ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update webhook log with FAILED status and error message")
    void handleFailure_updatesLogWithFailedStatusAndMessage() {
        // Arrange
        Exception ex = new RuntimeException("Connection refused");

        // Act
        webhookLogService.handleFailure(webhookLog, ex);

        // Assert
        assertThat(webhookLog.getEventStatus()).isEqualTo(WebhookEventStatus.FAILED);
        assertThat(webhookLog.getHttpStatus()).isNull();
        assertThat(webhookLog.getResponseBody()).isEqualTo("Connection refused");
        verify(webhookLogRepository).save(webhookLog);
    }

    @Test
    @DisplayName("Should save webhook log exactly once on failure")
    void handleFailure_savesLogExactlyOnce() {
        // Arrange
        Exception ex = new RuntimeException("Error");

        // Act
        webhookLogService.handleFailure(webhookLog, ex);

        // Assert
        verify(webhookLogRepository, times(1)).save(webhookLog);
    }

    @Test
    @DisplayName("Should set httpStatus to null on failure")
    void handleFailure_setsHttpStatusToNull() {
        // Arrange — pre-set a status to verify it gets cleared
        webhookLog.setHttpStatus(200);
        Exception ex = new RuntimeException("Timeout");

        // Act
        webhookLogService.handleFailure(webhookLog, ex);

        // Assert
        assertThat(webhookLog.getHttpStatus()).isNull();
    }

    @Test
    @DisplayName("Should handle null exception message without throwing")
    void handleFailure_nullExceptionMessage_savesLog() {
        // Arrange
        Exception ex = new RuntimeException((String) null);

        // Act
        webhookLogService.handleFailure(webhookLog, ex);

        // Assert
        assertThat(webhookLog.getEventStatus()).isEqualTo(WebhookEventStatus.FAILED);
        assertThat(webhookLog.getResponseBody()).isNull();
        verify(webhookLogRepository).save(webhookLog);
    }
}