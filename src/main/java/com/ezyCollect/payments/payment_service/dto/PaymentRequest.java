package com.ezyCollect.payments.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PaymentRequest (
        @NotBlank(message = "First name cannot be blank")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,
        @NotBlank(message = "Last name cannot be blank")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,
        @NotBlank(message = "ZIP code cannot be blank")
        @Size(max = 20, message = "ZIP code must not exceed 20 characters")
        String zipCode,

        /**
         * Ensure PCI compliance before storing any card information
         */
        @NotBlank(message = "Card number cannot be blank")
        @Pattern(regexp = "\\d{13,19}", message = "Card number must be between 13 and 19 digits")
        String cardNumber
) {}
