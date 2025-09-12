package com.fintech.userservice.dto.message;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreationMessage {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "FirstName is required")
    @Size(min = 2, max = 100, message = "FirstName must be between 2 and 100 characters")
    private String firstName;
    @NotBlank(message = "LastName is required")
    @Size(min = 2, max = 100, message = "LastName must be between 2 and 100 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    @NotBlank(message = "Occupation is required")
    private String occupation;

    @DecimalMin(value = "0.0", message = "Initial deposit must be non-negative")
    private Double initialDeposit;

    private String role = "ACCOUNT_HOLDER"; // Default role

    // Default constructor
    public UserCreationMessage() {
    }

    public UserCreationMessage(String userId, String firstName, String lastName, String email, String phoneNumber, String address, String dateOfBirth, String occupation, Double initialDeposit) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.initialDeposit = initialDeposit;
    }

    public String getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getOccupation() {
        return occupation;
    }

    public Double getInitialDeposit() {
        return initialDeposit;
    }

    public String getRole() {
        return role;
    }
}
