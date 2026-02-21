package com.ezyCollect.payments.payment_service.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String errorCode;
    private String message;
    private int status;
    private String path;
    private LocalDateTime timestamp;
}
