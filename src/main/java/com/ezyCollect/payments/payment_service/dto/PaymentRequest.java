package com.ezyCollect.payments.payment_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "First name cannot be blank")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "ZIP code cannot be blank")
    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    private String zipCode;

    /**
     * Ensure PCI compliance before storing any card information
     */
    @NotBlank(message = "Card number cannot be blank")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be between 13 and 19 digits")
    private String cardNumber;
}
