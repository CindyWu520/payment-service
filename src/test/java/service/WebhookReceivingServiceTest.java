package service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.service.WebhookReceivingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class WebhookReceivingServiceTest {

    private WebhookLogRepository webhookLogRepository;
    private ObjectMapper objectMapper;
    private WebhookReceivingService webhookReceivingService;

    @BeforeEach
    void setUp() {
        webhookLogRepository = mock(WebhookLogRepository.class);
        objectMapper = new ObjectMapper();
        webhookReceivingService = new WebhookReceivingService(webhookLogRepository, objectMapper);
    }

    @Test
    void processWebhookAsync_shouldSaveWebhookLog() throws Exception {
        // Arrange
        PaymentResponse payload = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .errorMessage(null)
                .build();

        String url = "https://example.com/webhook";

        // Act
        webhookReceivingService.processWebhookAsync(payload, url);

        // Assert
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository, times(1)).save(captor.capture());

        WebhookLog savedLog = captor.getValue();
        assertEquals(url, savedLog.getUrl());
        assertEquals(WebhookDirection.INCOMING, savedLog.getDirection());
        assertEquals(WebhookEventStatus.RECEIVED, savedLog.getEventStatus());
        assertEquals(200, savedLog.getHttpStatus());
        assertEquals("Webhook received successfully", savedLog.getResponseBody());
        assertEquals(0, savedLog.getRetryCount());

        // Verify payload was serialized correctly
        assertTrue(savedLog.getPayload().contains("\"transactionId\":\"tx123\""));
        assertTrue(savedLog.getPayload().contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void processWebhookAsync_WebhookException() {
        // Arrange
        PaymentResponse payload = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .errorMessage(null)
                .build();

        String url = "https://example.com/webhook";

        // Make the repository throw JsonProcessingException wrapped in WebhookException
        doThrow(new WebhookException("DB failure"))
                .when(webhookLogRepository).save(any(WebhookLog.class));

        // Act & Assert
        WebhookException exception = assertThrows(WebhookException.class,
                () -> webhookReceivingService.processWebhookAsync(payload, url));

        assertEquals("Failed to process webhook", exception.getMessage());
    }
}