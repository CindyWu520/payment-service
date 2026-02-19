package com.ezyCollect.payments.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    /**
     * SUCCESS / FAILED
     */
    private String status;

    /**
     * Transaction ID (saved payment ID from DB)
     */
    private String transactionId;

    /**
     * Optional: add error message if failed
     */
    private String errorMessage;
}
