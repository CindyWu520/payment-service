package service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Webhook;
import com.ezyCollect.payments.payment_service.entity.WebhookLog;
import com.ezyCollect.payments.payment_service.enums.WebhookEventStatus;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.WebhookLogRepository;
import com.ezyCollect.payments.payment_service.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookLogRepository webhookLogRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private Webhook webhook;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        webhook = Webhook.builder()
                .id(1L)
                .url("http://example.com/webhook")
                .active(true)
                .build();

        paymentResponse = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .build();
    }

    @Test
    void testSendWebhookSuccess() throws Exception {
        // Mock JSON serialization
        when(objectMapper.writeValueAsString(paymentResponse)).thenReturn("{\"transactionId\":\"tx123\"}");

        // Mock saving the log
        when(webhookLogRepository.save(any()))
                .thenAnswer(i -> i.getArguments()[0]);

        // Mock REST call returns 200 OK
        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        // Call the method
        webhookService.sendWebhook(webhook, paymentResponse);

        // Verify webhook log saved and updated
        verify(webhookLogRepository, atLeastOnce()).save(any());

        // Verify REST call was mocked, no real call
        verify(restTemplate).postForEntity(eq(webhook.getUrl()), eq(paymentResponse), eq(String.class));
    }

    @Test
    void testSendWebhookFailureNon2xx() throws Exception {
        // Mock JSON serialization
        when(objectMapper.writeValueAsString(paymentResponse)).thenReturn("{\"transactionId\":\"tx123\"}");

        // Mock saving the log
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // Mock REST call returns 500
        ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.sendWebhook(webhook, paymentResponse));

        assertTrue(ex.getMessage().contains("Non-2xx response"));

        // Verify webhook log updated with failure
        verify(webhookLogRepository, atLeastOnce()).save(argThat(log ->
                log.getEventStatus() == WebhookEventStatus.FAILED &&
                        log.getRetryCount() == 1
        ));
    }

    @Test
    void testSendWebhookNetworkException() throws Exception {
        // Mock JSON serialization
        when(objectMapper.writeValueAsString(paymentResponse)).thenReturn("{\"transactionId\":\"tx123\"}");

        // Mock saving the log
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // Mock REST call throws exception (simulate network error)
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        WebhookException ex = assertThrows(WebhookException.class,
                () -> webhookService.sendWebhook(webhook, paymentResponse));

        assertTrue(ex.getMessage().contains("Network error"));

        // Verify webhook log updated with failure
        verify(webhookLogRepository, atLeastOnce()).save(argThat(log ->
                log.getEventStatus() == WebhookEventStatus.FAILED &&
                        log.getRetryCount() == 1 &&
                        log.getResponseBody().contains("Network error")
        ));
    }
}