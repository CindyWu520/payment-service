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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;
    private final CardEncryptionService cardEncryptionService;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {

        Payment payment = buildEncryptedPayment(request);
        processPaymentViaGateway(payment);

        // TODO: Add metrics to track payment success and failure counts

        Payment savedPayment = savePayment(payment);
        PaymentResponse response = buildSuccessResponse(savedPayment);

        try {
            webhookService.triggerWebhooks(response);
        } catch (WebhookException e) {
            // webhook failure should NOT fail the payment
            log.warn("Webhook failed for payment {} : {}",
                    savedPayment.getId(), e.getMessage());
        }

        return response;
    }

    private void processPaymentViaGateway(Payment payment) {
        // TODO : Process the payment via a payment gateway, e.g. Stripe, PayPal
        boolean gatewaySuccess = true;
        if (!gatewaySuccess) {
            throw new PaymentException(ErrorCode.GATEWAY_TIMEOUT);
        }
    }

    private Payment buildEncryptedPayment(PaymentRequest request) {
        try {
            EncryptedCardInfo encryptedCardInfo = cardEncryptionService.encryptCard(request.cardNumber());

            return Payment.builder()
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .zipCode(request.zipCode())
                    .cardNumber(encryptedCardInfo.encryptedCard())
                    .iv(encryptedCardInfo.iv())
                    .build();

        } catch (EncryptionException e) {
            log.error("Card Encryption error during payment: {}", e.getMessage());
            throw new PaymentException(ErrorCode.CARD_ENCRYPTION_ERROR, e);
        }
    }

    @Transactional
    public Payment savePayment(Payment payment) {
        try {
            return paymentRepository.save(payment);
        } catch (DataAccessException e) {
            log.error("Database error during payment: {}", e.getMessage());
            throw new PaymentException(ErrorCode.DATABASE_ERROR, e);
        }
    }

    private PaymentResponse buildSuccessResponse(Payment savedPayment) {
        return PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId(savedPayment.getId().toString())
                .build();
    }
}
