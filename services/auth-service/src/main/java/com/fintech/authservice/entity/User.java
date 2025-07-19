package com.fintech.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_account_number", columnList = "accountNumber"),
    @Index(name = "idx_user_phone", columnList = "phoneNumber"),
    @Index(name = "idx_user_user_id", columnList = "userId"),
    @Index(name = "idx_user_shard_key", columnList = "shardKey")
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Unique user identifier for sharding
    @Column(nullable = false, unique = true, length = 255)
    private String userId;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Column(nullable = false)
    private String password;
    
    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @NotBlank(message = "Address is required")
    @Column(nullable = false, length = 500)
    private String address;
    
    @NotBlank(message = "Date of birth is required")
    @Column(nullable = false)
    private String dateOfBirth;
    
    @NotBlank(message = "Occupation is required")
    @Column(nullable = false, length = 100)
    private String occupation;
    
    @Column(nullable = false)
    private Double initialDeposit = 0.0;
    
    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;
    
    @Column(nullable = false)
    private Double accountBalance = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.ACCOUNT_HOLDER;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column
    private String refreshToken;
    
    @Column
    private LocalDateTime refreshTokenExpiryDate;
    
    @Column
    private String currentSessionId;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Sharding key - based on account number hash
    @Column(nullable = false)
    private Integer shardKey;
    
    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }
    
    public enum UserRole {
        ACCOUNT_HOLDER("ROLE_ACCOUNT_HOLDER", "Account Holder"),
        SALES_PERSON("ROLE_SALES_PERSON", "Sales Person"), 
        MANAGER("ROLE_MANAGER", "Manager");
        
        private final String authority;
        private final String displayName;
        
        UserRole(String authority, String displayName) {
            this.authority = authority;
            this.displayName = displayName;
        }
        
        public String getAuthority() {
            return authority;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructors
    public User() {}
    
    public User(String fullName, String email, String password, String phoneNumber, 
                String address, String dateOfBirth, String occupation, Double initialDeposit) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.initialDeposit = initialDeposit;
        this.accountBalance = initialDeposit;
    }
    
    // Getters and Setters
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public Double getAccountBalance() {
        return accountBalance;
    }
    
    public void setAccountBalance(Double accountBalance) {
        this.accountBalance = accountBalance;
    }
    
    public AccountStatus getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public LocalDateTime getRefreshTokenExpiryDate() {
        return refreshTokenExpiryDate;
    }
    
    public void setRefreshTokenExpiryDate(LocalDateTime refreshTokenExpiryDate) {
        this.refreshTokenExpiryDate = refreshTokenExpiryDate;
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
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
    
    public Integer getShardKey() {
        return shardKey;
    }
    
    public void setShardKey(Integer shardKey) {
        this.shardKey = shardKey;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
}
