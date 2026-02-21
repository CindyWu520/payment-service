package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.exception.EncryptionException;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import com.ezyCollect.payments.payment_service.util.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;
    private final SecretKey secretKey;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              WebhookService webhookService,
                              @Value("${aes.secret.key}") String secretKeyString) {
        this.paymentRepository = paymentRepository;
        this.webhookService = webhookService;
        this.secretKey = AESUtil.decodeKeyFromBase64(secretKeyString);
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Generate IV per payment
            byte[] iv = AESUtil.generateRandomIV();
            // Encrypt the card number
            String encryptedCard = AESUtil.encrypt(request.getCardNumber(), secretKey, iv);
            // Convert IV to Base64 for DB storage
            String ivBase64 = Base64.getEncoder().encodeToString(iv);

            // Build the payment entity
            Payment payment = Payment.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .zipCode(request.getZipCode())
                    .cardNumber(encryptedCard)
                    .iv(ivBase64)
                    .build();

            // Process the payment via a payment gateway, e.g. Stripe, PayPal

            // Save payment in database
            Payment savedPayment = paymentRepository.save(payment);

            // Build response
            PaymentResponse response = PaymentResponse.builder()
                    .status("SUCCESS")
                    .transactionId(savedPayment.getId().toString())
                    .build();

            // Trigger all registered webhooks asynchronously
            webhookService.triggerWebhooks(response);

            return response;

        } catch (EncryptionException e) {
            throw new PaymentException(
                    "PAYMENT_ENCRYPTION_FAILED",
                    "Failed to encrypt card number",
                    e
            );
        } catch (DataAccessException e) {
            throw new PaymentException(
                    "PAYMENT_PERSISTENCE_FAILED",
                    "Failed to save payment record",
                    e
            );
        }
    }
}
