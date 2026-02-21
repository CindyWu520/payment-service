package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;
    private final CardEncryptionService cardEncryptionService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              WebhookService webhookService,
                              CardEncryptionService cardEncryptionService) {
        this.paymentRepository = paymentRepository;
        this.webhookService = webhookService;
        this.cardEncryptionService = cardEncryptionService;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        Payment payment = buildEncryptedPayment(request);

        // TODO: Process the payment via a payment gateway, e.g. Stripe, PayPal
        // TODO: Add metrics to track payment success and failure counts

        Payment savedPayment = savePayment(payment);

        // This line will not be reached if the payment gateway call fails
        PaymentResponse response = buildSuccessResponse(savedPayment);

        webhookService.triggerWebhooks(response);

        return response;
    }

    private Payment buildEncryptedPayment(PaymentRequest request) {
        try {
            EncryptedCardInfo encryptedCardInfo = cardEncryptionService.encryptCard(request.getCardNumber());

            return Payment.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .zipCode(request.getZipCode())
                    .cardNumber(encryptedCardInfo.encryptedCard())
                    .iv(encryptedCardInfo.iv())
                    .build();

        } catch (EncryptionException e) {
            throw new PaymentException(
                    "PAYMENT_ENCRYPTION_FAILED",
                    "Failed to encrypt card number",
                    e
            );
        }
    }

    @Transactional
    private Payment savePayment(Payment payment) {
        try {
            return paymentRepository.save(payment);
        } catch (DataAccessException e) {
            throw new PaymentException(
                    "PAYMENT_PERSISTENCE_FAILED",
                    "Failed to save payment record",
                    e
            );
        }
    }

    private PaymentResponse buildSuccessResponse(Payment savedPayment) {
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(savedPayment.getId().toString())
                .build();
    }
}
