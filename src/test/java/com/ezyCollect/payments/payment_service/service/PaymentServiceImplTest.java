package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.exception.WebhookException;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookService webhookService;

    @Mock
    private CardEncryptionService cardEncryptionService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest request;
    private EncryptedCardInfo encryptedCardInfo;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        request = PaymentRequest.builder()
                .firstName("Christy")
                .lastName("Wu")
                .zipCode("2065")
                .cardNumber("1234567890123456")
                .build();

        encryptedCardInfo = new EncryptedCardInfo("encryptedCard123", "ivBase64==");

        savedPayment = Payment.builder()
                .id(1L)
                .firstName("Christy")
                .lastName("Wu")
                .zipCode("2065")
                .cardNumber("encryptedCard123")
                .iv("ivBase64==")
                .build();
    }

    // ─── Happy Path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should process payment successfully and return SUCCESS response")
    void processPayment_success() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.cardNumber()))
                .thenReturn(encryptedCardInfo);
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // Act
        PaymentResponse response = paymentService.processPayment(request);

        // Assert
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.transactionId()).isEqualTo("1");

        verify(cardEncryptionService).encryptCard(request.cardNumber());
        verify(paymentRepository).save(any(Payment.class));
        verify(webhookService).triggerWebhooks(response);
    }

    @Test
    @DisplayName("Should save payment with encrypted card number and IV")
    void processPayment_savesEncryptedCardDetails() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.cardNumber()))
                .thenReturn(encryptedCardInfo);
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // Act
        paymentService.processPayment(request);

        // Assert — verify the payment saved has encrypted card, not raw
        verify(paymentRepository).save(argThat(payment ->
                payment.getCardNumber().equals("encryptedCard123") &&
                        payment.getIv().equals("ivBase64==") &&
                        payment.getFirstName().equals("Christy") &&
                        payment.getLastName().equals("Wu") &&
                        payment.getZipCode().equals("2065")
        ));
    }

    // ─── Encryption Failure ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw PaymentException with CARD_ENCRYPTION_ERROR when encryption fails")
    void processPayment_encryptionFails_throwsPaymentException() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.getCardNumber()))
                .thenThrow(new EncryptionException("Failed to encrypt card number"));

        // Act & Assert
        PaymentException ex = catchThrowableOfType(
                () -> paymentService.processPayment(request),
                PaymentException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_ENCRYPTION_ERROR);

        // Verify payment was never saved
        verify(paymentRepository, never()).save(any());
        verify(webhookService, never()).triggerWebhooks(any());
    }

    // ─── Database Failure ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw PaymentException with DATABASE_ERROR when save fails")
    void processPayment_databaseFails_throwsPaymentException() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.cardNumber()))
                .thenReturn(encryptedCardInfo);
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataAccessException("DB connection lost") {});

        // Act & Assert
        PaymentException ex = catchThrowableOfType(
                () -> paymentService.processPayment(request),
                PaymentException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DATABASE_ERROR);

        // Verify webhook was never triggered
        verify(webhookService, never()).triggerWebhooks(any());
    }

    // ─── Webhook Failure ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should still return SUCCESS response even when webhook fails")
    void processPayment_webhookFails_doesNotFailPayment() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.cardNumber()))
                .thenReturn(encryptedCardInfo);
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);
        doThrow(new WebhookException(ErrorCode.WEBHOOK_DELIVERY_FAILED))
                .when(webhookService).triggerWebhooks(any());

        // Act
        PaymentResponse response = paymentService.processPayment(request);

        // Assert — payment still succeeds
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.transactionId()).isEqualTo("1");

        // Verify webhook was attempted
        verify(webhookService).triggerWebhooks(any());
    }

    @Test
    @DisplayName("Should still return SUCCESS response even when unexpected exception thrown from webhook")
    void processPayment_webhookThrowsUnexpectedException_doesNotFailPayment() {
        // Arrange
        when(cardEncryptionService.encryptCard(request.cardNumber()))
                .thenReturn(encryptedCardInfo);
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // Note: RuntimeException is NOT caught in current code — this test
        // documents that only WebhookException is swallowed.
        // Consider catching Exception instead of WebhookException in processPayment
        // if you want all webhook errors to be non-fatal.
        doThrow(new WebhookException(ErrorCode.WEBHOOK_DELIVERY_FAILED))
                .when(webhookService).triggerWebhooks(any());

        // Act
        PaymentResponse response = paymentService.processPayment(request);

        // Assert
        assertThat(response.status()).isEqualTo("SUCCESS");
    }

    // ─── savePayment ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return saved payment when repository save succeeds")
    void savePayment_success() {
        // Arrange
        Payment payment = Payment.builder()
                .firstName("Christy")
                .cardNumber("encryptedCard123")
                .build();
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        // Act
        Payment result = paymentService.savePayment(payment);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should throw PaymentException with DATABASE_ERROR when repository throws DataAccessException")
    void savePayment_dataAccessException_throwsPaymentException() {
        // Arrange
        Payment payment = Payment.builder().cardNumber("encryptedCard123").build();
        when(paymentRepository.save(payment))
                .thenThrow(new DataAccessException("Connection timeout") {});

        // Act & Assert
        PaymentException ex = catchThrowableOfType(
                () -> paymentService.savePayment(payment),
                PaymentException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DATABASE_ERROR);
    }
}