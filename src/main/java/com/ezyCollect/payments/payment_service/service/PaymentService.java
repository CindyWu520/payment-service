package com.ezyCollect.payments.payment_service.service;

import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;

public interface PaymentService {
    /**
     * Process a payment request
     * @param request PaymentRequest DTO
     * @return PaymentResponse DTO
     */
    PaymentResponse processPayment(PaymentRequest request);
}
