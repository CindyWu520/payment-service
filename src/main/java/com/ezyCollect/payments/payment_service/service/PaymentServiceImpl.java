package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.entity.Payment;
import com.ezyCollect.payments.payment_service.repository.PaymentRepository;
import com.ezyCollect.payments.payment_service.util.AESUtil;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;
    private final SecretKey secretKey;
    private final byte[] iv;

    public PaymentServiceImpl(PaymentRepository paymentRepository) throws Exception {
        this.paymentRepository = paymentRepository;
        this.secretKey = AESUtil.generateKey();
        this.iv = new byte[12]; // For simplicity, generate IV properly in production
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Encrypt the card number
            String encryptedCard = AESUtil.encrypt(request.getCardNumber(), secretKey, iv);

            // Build the payment entity
            Payment payment = Payment.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .zipCode(request.getZipCode())
                    .cardNumber(encryptedCard)
                    .build();

            // Save payment in database
            Payment savedPayment = paymentRepository.save(payment);

            // Build response
            return PaymentResponse.builder()
                    .status("SUCCESS")
                    .transactionId(savedPayment.getId().toString())
                    .build();

        } catch (Exception e) {
            // Handle encryption or save failure
            throw new RuntimeException("Payment processing failed", e);
        }
    }
}
