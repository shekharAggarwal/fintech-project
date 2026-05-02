package com.fintech.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.request.OtpVerificationRequest;
import com.fintech.paymentservice.service.OtpService;
import com.fintech.security.service.AuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentOtpController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentOtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @MockBean
    private AuthorizationService authorizationService;

    private static final String USER_ID = "user-123";

    // --- POST /api/payments/{paymentId}/verify-otp ---

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 200 on valid OTP")
    void verifyOtp_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(otpService.verifyOtp(eq("PAY001"), eq("123456"), eq(USER_ID))).thenReturn(true);

        OtpVerificationRequest request = new OtpVerificationRequest("123456");

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"))
                .andExpect(jsonPath("$.paymentId").value("PAY001"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 400 on invalid OTP")
    void verifyOtp_invalidOtp() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(otpService.verifyOtp(eq("PAY001"), eq("000000"), eq(USER_ID))).thenReturn(false);

        OtpVerificationRequest request = new OtpVerificationRequest("000000");

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid OTP"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 400 when no user context")
    void verifyOtp_noUserContext() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(null);

        OtpVerificationRequest request = new OtpVerificationRequest("123456");

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user context"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 500 on exception")
    void verifyOtp_exception() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(otpService.verifyOtp(eq("PAY001"), eq("123456"), eq(USER_ID)))
                .thenThrow(new RuntimeException("Redis connection failed"));

        OtpVerificationRequest request = new OtpVerificationRequest("123456");

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("OTP verification failed"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 400 for invalid OTP format")
    void verifyOtp_invalidFormat() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);

        // OTP must be 6 digits
        String invalidJson = "{\"otp\":\"abc\"}";

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify-otp returns 400 for blank OTP")
    void verifyOtp_blankOtp() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);

        String invalidJson = "{\"otp\":\"\"}";

        mockMvc.perform(post("/api/payments/PAY001/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
