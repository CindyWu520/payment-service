package com.ezyCollect.payments.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private String firstName;
    private String lastName;
    private String zipCode;

    /**
     * Card number will be received from client as plain text,
     * then encrypted in the service layer before storing.
     */
    private String cardNumber;
}
