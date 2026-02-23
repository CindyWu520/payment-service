package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookReceivingServiceTest {

    @Mock
    private WebhookLogRepository webhookLogRepository;

    @InjectMocks
    private WebhookReceivingService webhookReceivingService;

    // Use real ObjectMapper — no need to mock serialization
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentResponse paymentResponse;
    private static final String WEBHOOK_URL = "http://localhost:8080/v1/webhooks/receive";

    @BeforeEach
    void setUp() throws Exception {
        // Inject real ObjectMapper
        var field = WebhookReceivingService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(webhookReceivingService, objectMapper);

        paymentResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();
    }

    // ─── Happy Path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save webhook log with correct fields when payload is valid")
    void processWebhookAsync_success_savesLogWithCorrectFields() {
        // Act
        webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL);

        // Assert — capture the saved log and verify all fields
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());

        WebhookLog savedLog = captor.getValue();
        assertThat(savedLog.getUrl()).isEqualTo(WEBHOOK_URL);
        assertThat(savedLog.getDirection()).isEqualTo(WebhookDirection.INCOMING);
        assertThat(savedLog.getEventStatus()).isEqualTo(WebhookEventStatus.RECEIVED);
        assertThat(savedLog.getHttpStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(savedLog.getResponseBody()).isEqualTo("Webhook received successfully");
        assertThat(savedLog.getReceiveAt()).isNotNull();
    }

    @Test
    @DisplayName("Should serialize payload to JSON and save in webhook log")
    void processWebhookAsync_success_savesSerializedPayload() throws Exception {
        // Act
        webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL);

        // Assert — payload is JSON string of the PaymentResponse
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());

        String expectedPayload = objectMapper.writeValueAsString(paymentResponse);
        assertThat(captor.getValue().getPayload()).isEqualTo(expectedPayload);
    }

    @Test
    @DisplayName("Should save webhook log once for each webhook received")
    void processWebhookAsync_success_savesExactlyOnce() {
        // Act
        webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL);

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    // ─── Failure Handling ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should not throw exception when repository save fails — error is swallowed")
    void processWebhookAsync_repositoryFails_doesNotThrow() {
        // Arrange
        doThrow(new RuntimeException("DB connection lost"))
                .when(webhookLogRepository).save(any());

        // Act & Assert — should NOT propagate exception (caught internally)
        assertThatCode(() ->
                webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should not throw exception when ObjectMapper serialization fails")
    void processWebhookAsync_serializationFails_doesNotThrow() throws Exception {
        // Arrange — inject a broken ObjectMapper that always fails
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        var field = WebhookReceivingService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(webhookReceivingService, brokenMapper);

        // Act & Assert — exception caught internally, nothing propagates
        assertThatCode(() ->
                webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL)
        ).doesNotThrowAnyException();

        // Verify log was never saved since serialization failed before save
        verify(webhookLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save log when serialization fails")
    void processWebhookAsync_serializationFails_neverSavesLog() throws Exception {
        // Arrange
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        var field = WebhookReceivingService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(webhookReceivingService, brokenMapper);

        // Act
        webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL);

        // Assert
        verify(webhookLogRepository, never()).save(any());
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null transactionId in payload without throwing")
    void processWebhookAsync_nullTransactionId_doesNotThrow() {
        // Arrange
        PaymentResponse nullTransactionResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(null)
                .build();

        // Act & Assert
        assertThatCode(() ->
                webhookReceivingService.processWebhookAsync(nullTransactionResponse, WEBHOOK_URL)
        ).doesNotThrowAnyException();

        verify(webhookLogRepository).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should process multiple webhooks independently")
    void processWebhookAsync_calledMultipleTimes_savesEachLog() {
        // Arrange
        PaymentResponse secondResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("99")
                .build();

        // Act
        webhookReceivingService.processWebhookAsync(paymentResponse, WEBHOOK_URL);
        webhookReceivingService.processWebhookAsync(secondResponse, WEBHOOK_URL);

        // Assert — saved once per call
        verify(webhookLogRepository, times(2)).save(any(WebhookLog.class));
    }
}