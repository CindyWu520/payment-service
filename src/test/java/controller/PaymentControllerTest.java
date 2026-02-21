package controller;

import com.ezyCollect.payments.payment_service.PaymentServiceApplication;
import com.ezyCollect.payments.payment_service.dto.PaymentRequest;
import com.ezyCollect.payments.payment_service.dto.PaymentResponse;
import com.ezyCollect.payments.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PaymentServiceApplication.class)
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayment_success() throws Exception {

        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .firstName("firstName")
                .lastName("lastName")
                .zipCode("2037")
                .cardNumber("1234567890987")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("tx123")
                .status("SUCCESS")
                .errorMessage(null)
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService, times(1))
                .processPayment(any(PaymentRequest.class));
    }

    @Test
    void createPayment_validationFail_shouldReturn400() throws Exception {

        // Missing required fields
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .firstName(null)   // assuming @NotNull
                .lastName(null)
                .zipCode(null)
                .cardNumber(null)
                .build();

        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never())
                .processPayment(any());
    }
}