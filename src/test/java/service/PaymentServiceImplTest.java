package service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import com.ezyCollect.payments.payment_service.service.CardEncryptionService;
import com.ezyCollect.payments.payment_service.service.PaymentServiceImpl;
import com.ezyCollect.payments.payment_service.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookService webhookService;

    @Mock
    private CardEncryptionService cardEncryptionService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessPaymentSuccess() throws Exception {
        // Prepare request
        PaymentRequest request = PaymentRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .zipCode("12345")
                .cardNumber("4111111111111111")
                .build();

        // Mock encryption
        EncryptedCardInfo encryptedCardInfo = new EncryptedCardInfo("encryptedCard", "ivBase64");
        when(cardEncryptionService.encryptCard(anyString())).thenReturn(encryptedCardInfo);

        // Mock repository save
        Payment savedPayment = Payment.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .zipCode("12345")
                .cardNumber("encryptedCard")
                .iv("ivBase64")
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Call service
        PaymentResponse response = paymentService.processPayment(request);

        // Verify repository save
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();
        assertEquals("John", capturedPayment.getFirstName());
        assertEquals("Doe", capturedPayment.getLastName());
        assertEquals("encryptedCard", capturedPayment.getCardNumber());
        assertEquals("ivBase64", capturedPayment.getIv());

        // Verify webhook triggered
        verify(webhookService).triggerWebhooks(response);

        // Assert response
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("1", response.getTransactionId());
    }

    @Test
    void testProcessPaymentEncryptionFailure() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .zipCode("12345")
                .cardNumber("4111111111111111")
                .build();

        // Simulate encryption failure
        when(cardEncryptionService.encryptCard(anyString()))
                .thenThrow(new EncryptionException("fail"));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processPayment(request));
        assertEquals("PAYMENT_ENCRYPTION_FAILED", ex.getErrorCode());
    }

    @Test
    void testProcessPaymentPersistenceFailure() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .zipCode("12345")
                .cardNumber("4111111111111111")
                .build();

        // Mock encryption success
        EncryptedCardInfo encryptedCardInfo = new EncryptedCardInfo("encryptedCard", "ivBase64");
        when(cardEncryptionService.encryptCard(anyString())).thenReturn(encryptedCardInfo);

        // Simulate repository failure
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataAccessException("DB fail") {});

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processPayment(request));
        assertEquals("PAYMENT_PERSISTENCE_FAILED", ex.getErrorCode());
    }

}
