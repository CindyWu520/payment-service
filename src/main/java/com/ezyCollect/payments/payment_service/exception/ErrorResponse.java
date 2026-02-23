package com.ezyCollect.payments.payment_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @param errorCode custom code. e.g. "PAYMENT_DECLINED"
 * @param message   user-friendly message
 */

@Builder
public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        int status,
        LocalDateTime timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> fieldErrors
) {}
