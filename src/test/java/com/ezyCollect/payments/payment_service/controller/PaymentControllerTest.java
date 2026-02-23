package com.ezyCollect.payments.payment_service.controller;

import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.exception.ErrorCode;
import com.ezyCollect.payments.payment_service.exception.GlobalExceptionHandler;
import com.ezyCollect.payments.payment_service.exception.PaymentException;
import com.ezyCollect.payments.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PaymentController.class, GlobalExceptionHandler.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private PaymentResponse successResponse;

    private static final String URL = "/v1/payments";

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequest.builder()
                .firstName("Christy")
                .lastName("Wu")
                .zipCode("2065")
                .cardNumber("1234567890123456")
                .build();

        successResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("42")
                .build();
    }

    // ─── Happy Path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 OK with SUCCESS response when payment is processed")
    void createPayment_success_returns200() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("42"));

        verify(paymentService).processPayment(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("Should pass correct request fields to service")
    void createPayment_success_passesCorrectFieldsToService() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenReturn(successResponse);

        // Act
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // Assert — verify service received correct request
        verify(paymentService).processPayment(argThat(request ->
                request.firstName().equals("Christy") &&
                        request.lastName().equals("Wu") &&
                        request.zipCode().equals("2065") &&
                        request.cardNumber().equals("1234567890123456")
        ));
    }

    // ─── Validation Errors ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 400 when firstName is blank")
    void createPayment_blankFirstName_returns400() throws Exception {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("")
                .lastName("Wu")
                .zipCode("2065")
                .cardNumber("1234567890123456")
                .build();

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("Should return 400 when lastName is blank")
    void createPayment_blankLastName_returns400() throws Exception {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("Christy")
                .lastName("")
                .zipCode("2065")
                .cardNumber("1234567890123456")
                .build();

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.lastName").exists());

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("Should return 400 when cardNumber is blank")
    void createPayment_blankCardNumber_returns400() throws Exception {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("Christy")
                .lastName("Wu")
                .zipCode("2065")
                .cardNumber("")
                .build();

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.cardNumber").exists());

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("Should return 400 when zipCode is blank")
    void createPayment_blankZipCode_returns400() throws Exception {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("Christy")
                .lastName("Wu")
                .zipCode("")
                .cardNumber("1234567890123456")
                .build();

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.zipCode").exists());

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("Should return 400 with all field errors when all fields are blank")
    void createPayment_allFieldsBlank_returns400WithAllFieldErrors() throws Exception {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("")
                .lastName("")
                .zipCode("")
                .cardNumber("")
                .build();

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.zipCode").exists())
                .andExpect(jsonPath("$.fieldErrors.cardNumber").exists());

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("Should return 400 when request body is missing")
    void createPayment_missingRequestBody_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).processPayment(any());
    }

    // ─── Service Exceptions ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 422 with CARD_ENCRYPTION_ERROR when encryption fails")
    void createPayment_encryptionFails_returns422() throws Exception {
        // Arrange
        when(paymentService.processPayment(any()))
                .thenThrow(new PaymentException(ErrorCode.CARD_ENCRYPTION_ERROR));

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("CARD_ENCRYPTION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    @DisplayName("Should return 500 with DATABASE_ERROR when database fails")
    void createPayment_databaseFails_returns500() throws Exception {
        // Arrange
        when(paymentService.processPayment(any()))
                .thenThrow(new PaymentException(ErrorCode.DATABASE_ERROR));

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("DATABASE_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    @DisplayName("Should return 500 with INTERNAL_SERVER_ERROR on unexpected exception")
    void createPayment_unexpectedException_returns500() throws Exception {
        // Arrange
        when(paymentService.processPayment(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    @DisplayName("Should return consistent ErrorResponse structure on failure")
    void createPayment_failure_returnsConsistentErrorStructure() throws Exception {
        // Arrange
        when(paymentService.processPayment(any()))
                .thenThrow(new PaymentException(ErrorCode.DATABASE_ERROR));

        // Act & Assert — verify all ErrorResponse fields are present
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(URL));
    }
}