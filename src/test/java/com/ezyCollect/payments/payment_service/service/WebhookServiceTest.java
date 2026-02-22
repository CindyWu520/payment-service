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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookLogRepository webhookLogRepository;
    @Mock
    private WebhookRepository webhookRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private WebhookService webhookService;

    private Webhook webhook;
    private WebhookLog webhookLog;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        webhook = Webhook.builder()
                .id(1L)
                .url("http://example.com/webhook")
                .active(true)
                .build();

        webhookLog = webhookLog.builder()
                .id(1L)
                .webhookId(webhook.getId())
                .direction(WebhookDirection.OUTGOING)
                .url(webhook.getUrl())
                .payload("{\"transactionId\":\"tx123\"}")
                .eventStatus(WebhookEventStatus.PENDING)
                .httpStatus(200)
                .responseBody("Webhook sent successfully")
                .sentAt(LocalDateTime.now())
                .build();

        paymentResponse = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .build();
    }

    @Test
    void testRegisterWebhook_Success() {
        WebhookRequest request = new WebhookRequest(webhook.getUrl());
        when(webhookRepository.save(any(Webhook.class))).thenReturn(webhook);

        Webhook result = webhookService.registerWebhook(request);

        assertNotNull(result);
        assertEquals(webhook.getUrl(), result.getUrl());
        verify(webhookRepository).save(any(Webhook.class));
    }

    @Test
    void testRegisterWebhook_DatabaseError() {
        WebhookRequest request = new WebhookRequest(webhook.getUrl());
        when(webhookRepository.save(any(Webhook.class)))
                .thenThrow(new DataAccessException("DB down") {});

        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.registerWebhook(request));

        assertEquals(ErrorCode.DATABASE_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Failed to save webhook"));
    }

    @Test
    void testTriggerWebhooks_Success() throws Exception {
        when(webhookRepository.findAllByActiveTrue()).thenReturn(List.of(webhook));
        when(objectMapper.writeValueAsString(paymentResponse)).thenReturn("{\"transactionId\":\"tx123\"}");
        when(webhookLogRepository.save(any(WebhookLog.class))).thenReturn(webhookLog);
        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        webhookService.triggerWebhooks(paymentResponse);

        verify(webhookLogRepository, atLeastOnce()).save(any(WebhookLog.class));
        verify(restTemplate).postForEntity(eq(webhook.getUrl()), eq(paymentResponse), eq(String.class));
    }

    @Test
    void testSendWebhook_Non2xxResponse() throws Exception {
        ResponseEntity<String> response = new ResponseEntity<>("FAIL", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        WebhookLog log = new WebhookLog();
        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.sendWebhook(webhook, log, paymentResponse));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Non-2xx"));
    }

    @Test
    void testSendWebhook_NetworkError() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Timeout"));

        WebhookLog log = new WebhookLog();
        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.sendWebhook(webhook, log, paymentResponse));

        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Network error"));
    }

    @Test
    void testSendWebhook_HttpClientError() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("HTTP error"));

        WebhookLog log = new WebhookLog();
        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.sendWebhook(webhook, log, paymentResponse));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("HTTP client error"));
    }
}