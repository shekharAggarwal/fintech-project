package com.fintech.userservice.dto.message;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

public class UserCreationMessage {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
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
    public UserCreationMessage() {}
    
    // Constructor with all fields
    public UserCreationMessage(String userId, String fullName, String email, String phoneNumber, 
                             String address, String dateOfBirth, String occupation, 
                             Double initialDeposit, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.initialDeposit = initialDeposit;
        this.role = role;
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    
    public Double getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(Double initialDeposit) { this.initialDeposit = initialDeposit; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
