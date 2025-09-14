package com.fintech.userservice.entity;

import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @FieldAccessControl(resourceType = "user", fieldName = "firstName")
    private String firstName;

    @Column(nullable = false, length = 100)
    @FieldAccessControl(resourceType = "user", fieldName = "lastName")
    private String lastName;

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
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    @FieldAccessControl(resourceType = "user", fieldName = "updatedAt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    @FieldAccessControl(resourceType = "user", fieldName = "accountNumber", sensitive = true, redactedValue = "****")
    private String accountNumber;

    public UserProfile() {
    }

    public UserProfile(String userId, String firstName, String lastName, String email, String phoneNumber,
                       String address, String dateOfBirth, String occupation, Double initialDeposit,
                       String role, String accountNumber) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.initialDeposit = initialDeposit;
        this.role = role;
        this.accountNumber = accountNumber;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public void setInitialDeposit(Double initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
