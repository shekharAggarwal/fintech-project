package com.fintech.authservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String lastName,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "Date of birth is required")
        String dateOfBirth,

        @NotBlank(message = "Occupation is required")
        String occupation,

        @DecimalMin(value = "0.0", message = "Initial deposit must be non-negative")
        Double initialDeposit
) {
}
