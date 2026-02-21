package com.ezyCollect.payments.payment_service.controller;


import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping("/v1/payments")
    public PaymentResponse createPayment(@RequestBody @Valid PaymentRequest request) {
        return paymentService.processPayment(request);
    }
}
