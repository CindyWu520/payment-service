package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.EncryptedCardInfo;
import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
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
        Payment encryptedPayment;

        try {
            processPaymentViaGateway(payment);

            // TODO: Add metrics to track payment success and failure counts

            encryptedPayment = savePayment(payment);
        } catch (PaymentException e) {
            log.warn("Payment error occurred: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status(e.getHttpStatus().toString())
                    .errorMessage(e.getErrorCode().toString())
                    .build();
        }

        PaymentResponse response = buildSuccessResponse(encryptedPayment);

        webhookService.triggerWebhooks(response);

        return response;
    }

    private void processPaymentViaGateway(Payment payment) {
        // TODO: Process the payment via a payment gateway, e.g. Stripe, PayPal
        boolean gatewaySuccess = true;
        if (!gatewaySuccess) {
            throw new PaymentException(
                    ErrorCode.PAYMENT_DECLINED,
                    "Payment was declined by gateway",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
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
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
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
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
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
