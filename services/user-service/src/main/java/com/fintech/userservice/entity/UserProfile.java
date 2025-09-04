package com.fintech.userservice.entity;

import com.fintech.userservice.security.annotation.FieldAccessControl;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldAccessControl(resourceType = "user", fieldName = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    @FieldAccessControl(resourceType = "user", fieldName = "userId")
    private String userId;

    @Column(nullable = false, length = 100)
    @FieldAccessControl(resourceType = "user", fieldName = "fullName")
    private String fullName;

    @Column(nullable = false, length = 255)
    @FieldAccessControl(resourceType = "user", fieldName = "email", sensitive = true)
    private String email;

    @Column(nullable = false, length = 20)
    @FieldAccessControl(resourceType = "user", fieldName = "phoneNumber", sensitive = true)
    private String phoneNumber;

    @Column(nullable = false, length = 255)
    @FieldAccessControl(resourceType = "user", fieldName = "address", sensitive = true)
    private String address;

    @Column(nullable = false, length = 20)
    @FieldAccessControl(resourceType = "user", fieldName = "dateOfBirth", sensitive = true, redactedValue = "XXXX-XX-XX")
    private String dateOfBirth;

    @Column(nullable = false, length = 100)
    @FieldAccessControl(resourceType = "user", fieldName = "occupation")
    private String occupation;

    @Column(nullable = false)
    @FieldAccessControl(resourceType = "user", fieldName = "initialDeposit", sensitive = true, redactedValue = "***")
    private Double initialDeposit;

    @Column(nullable = false, length = 30)
    @FieldAccessControl(resourceType = "user", fieldName = "role")
    private String role;

    @Column(nullable = false)
    @FieldAccessControl(resourceType = "user", fieldName = "createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @FieldAccessControl(resourceType = "user", fieldName = "updatedAt")
    private LocalDateTime updatedAt;

    @Column
    @FieldAccessControl(resourceType = "user", fieldName = "accountNumber", sensitive = true, redactedValue = "****")
    private String accountNumber;

    public UserProfile() {
    }

    public UserProfile(String userId, String fullName, String email, String phoneNumber, String address,
                       String dateOfBirth, String occupation, Double initialDeposit, String role) {
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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Double getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(Double initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
