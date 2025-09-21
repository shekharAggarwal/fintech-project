package com.fintech.paymentservice.dto.message;

import jakarta.validation.constraints.NotBlank;

/**
 * Event published when OTP notification needs to be sent
 */
public class OtpNotificationEvent {


    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "amount is required")
    private String amount;

    @NotBlank(message = "OTP is required")
    private String otp;

    // Default constructor
    public OtpNotificationEvent() {
    }


    public OtpNotificationEvent(String userId, String amount, String otp) {
        this.userId = userId;
        this.amount = amount;
        this.otp = otp;
    }
}