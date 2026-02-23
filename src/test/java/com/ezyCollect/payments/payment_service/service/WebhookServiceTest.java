package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.dto.WebhookRequest;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookDirection;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.repository.WebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookRepository webhookRepository;
    @Mock private WebhookLogRepository webhookLogRepository;
    @Mock private WebhookSenderService webhookSenderService;

    @InjectMocks
    private WebhookService webhookService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Webhook webhook;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Inject real ObjectMapper
        var field = WebhookService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(webhookService, objectMapper);

        webhook = Webhook.builder()
                .id(1L)
                .url("http://localhost:8080/v1/webhooks/receive")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        paymentResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();
    }

    // ─── triggerWebhooks ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save webhook log and trigger send for each active webhook")
    void triggerWebhooks_success_savesLogAndTriggersSend() {
        // Arrange
        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));
        when(webhookLogRepository.save(any(WebhookLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        webhookService.triggerWebhooks(paymentResponse);

        // Assert
        verify(webhookLogRepository).save(any(WebhookLog.class));
        verify(webhookSenderService).sendWebhook(eq(webhook), any(WebhookLog.class), eq(paymentResponse));
    }

    @Test
    @DisplayName("Should create webhook log with correct fields")
    void triggerWebhooks_createsLogWithCorrectFields() throws Exception {
        // Arrange
        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));
        when(webhookLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        webhookService.triggerWebhooks(paymentResponse);

        // Assert
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());

        WebhookLog savedLog = captor.getValue();
        assertThat(savedLog.getWebhookId()).isEqualTo(1L);
        assertThat(savedLog.getDirection()).isEqualTo(WebhookDirection.OUTGOING);
        assertThat(savedLog.getUrl()).isEqualTo(webhook.getUrl());
        assertThat(savedLog.getEventStatus()).isEqualTo(WebhookEventStatus.PENDING);
        assertThat(savedLog.getSentAt()).isNotNull();
        assertThat(savedLog.getPayload()).isEqualTo(objectMapper.writeValueAsString(paymentResponse));
    }

    @Test
    @DisplayName("Should do nothing when no active webhooks exist")
    void triggerWebhooks_noActiveWebhooks_doesNothing() {
        // Arrange
        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of());

        // Act
        webhookService.triggerWebhooks(paymentResponse);

        // Assert
        verify(webhookLogRepository, never()).save(any());
        verify(webhookSenderService, never()).sendWebhook(any(), any(), any());
    }

    @Test
    @DisplayName("Should save log and trigger send for each webhook when multiple active webhooks exist")
    void triggerWebhooks_multipleWebhooks_sendsToAll() {
        // Arrange
        Webhook webhook2 = Webhook.builder()
                .id(2L)
                .url("http://example.com/webhook")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook, webhook2));
        when(webhookLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        webhookService.triggerWebhooks(paymentResponse);

        // Assert
        verify(webhookLogRepository, times(2)).save(any(WebhookLog.class));
        verify(webhookSenderService, times(2)).sendWebhook(any(), any(), eq(paymentResponse));
    }

    @Test
    @DisplayName("Should throw WebhookException when payload serialization fails")
    void triggerWebhooks_serializationFails_throwsWebhookException() throws Exception {
        // Arrange
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        var field = WebhookService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(webhookService, brokenMapper);

        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookService.triggerWebhooks(paymentResponse),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_PAYLOAD_SERIALIZATION_FAILED);
        verify(webhookLogRepository, never()).save(any());
        verify(webhookSenderService, never()).sendWebhook(any(), any(), any());
    }

    // ─── registerWebhook ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should register webhook successfully when URL does not exist")
    void registerWebhook_success_returnsWebhook() {
        // Arrange
        WebhookRequest request = new WebhookRequest("http://example.com/webhook");
        when(webhookRepository.existsByUrl(request.url())).thenReturn(false);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(webhook);

        // Act
        Webhook result = webhookService.registerWebhook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(webhookRepository).save(any(Webhook.class));
    }

    @Test
    @DisplayName("Should save webhook with active=true, correct URL and createdAt")
    void registerWebhook_savesCorrectFields() {
        // Arrange
        WebhookRequest request = new WebhookRequest("http://example.com/webhook");
        when(webhookRepository.existsByUrl(request.url())).thenReturn(false);
        when(webhookRepository.save(any())).thenReturn(webhook);

        // Act
        webhookService.registerWebhook(request);

        // Assert
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(captor.capture());

        Webhook saved = captor.getValue();
        assertThat(saved.getUrl()).isEqualTo("http://example.com/webhook");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_ALREADY_EXISTS when URL is duplicate")
    void registerWebhook_duplicateUrl_throwsWebhookException() {
        // Arrange
        WebhookRequest request = new WebhookRequest("http://example.com/webhook");
        when(webhookRepository.existsByUrl(request.url())).thenReturn(true);

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookService.registerWebhook(request),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_ALREADY_EXISTS);
        verify(webhookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw WebhookException with WEBHOOK_REGISTER_FAILED when DB save fails")
    void registerWebhook_dbSaveFails_throwsWebhookException() {
        // Arrange
        WebhookRequest request = new WebhookRequest("http://example.com/webhook");
        when(webhookRepository.existsByUrl(request.url())).thenReturn(false);
        when(webhookRepository.save(any())).thenThrow(new DataAccessException("DB error") {});

        // Act & Assert
        WebhookException ex = catchThrowableOfType(
                () -> webhookService.registerWebhook(request),
                WebhookException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEBHOOK_REGISTER_FAILED);
    }

    @Test
    @DisplayName("Should not call save when duplicate URL check throws")
    void registerWebhook_duplicateUrl_neverCallsSave() {
        // Arrange
        WebhookRequest request = new WebhookRequest("http://example.com/webhook");
        when(webhookRepository.existsByUrl(request.url())).thenReturn(true);

        // Act
        catchThrowableOfType(
                () -> webhookService.registerWebhook(request),
                WebhookException.class
        );

        // Assert
        verify(webhookRepository, never()).save(any());
    }
}