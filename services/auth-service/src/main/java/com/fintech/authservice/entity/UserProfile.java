package com.fintech.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User profile entity - contains non-authentication user data
 * This should be in a separate service in production (User Service)
 * Included here for reference and transition purposes
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_profile_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_profile_phone", columnList = "phoneNumber"),
    @Index(name = "idx_profile_status", columnList = "profileStatus"),
    @Index(name = "idx_profile_verification", columnList = "verificationStatus")
})
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to AuthUser
    @Column(nullable = false, unique = true, length = 36)
    private String userId;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @Column(length = 50)
    private String firstName;
    
    @Column(length = 50)
    private String lastName;
    
    @Column(length = 50)
    private String middleName;
    
    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(length = 20)
    private String alternatePhoneNumber;
    
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Column(length = 100)
    private String occupation;
    
    @Column(length = 100)
    private String employer;
    
    @Column(length = 500)
    private String address;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 50)
    private String state;
    
    @Column(length = 20)
    private String postalCode;
    
    @Column(length = 50)
    private String country;
    
    // KYC and verification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    
    @Column(length = 50)
    private String nationalId; // SSN, Passport, etc.
    
    @Enumerated(EnumType.STRING)
    private IdType nationalIdType;
    
    @Column
    private LocalDate nationalIdExpiryDate;
    
    // Profile settings
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus profileStatus = ProfileStatus.ACTIVE;
    
    @Column(length = 10)
    private String preferredLanguage = "en";
    
    @Column(length = 50)
    private String timezone = "UTC";
    
    @Column(nullable = false)
    private Boolean marketingOptIn = false;
    
    @Column(nullable = false)
    private Boolean smsNotificationEnabled = true;
    
    @Column(nullable = false)
    private Boolean emailNotificationEnabled = true;
    
    // Profile completion tracking
    @Column(nullable = false)
    private Integer profileCompletionPercentage = 0;
    
    @Column
    private LocalDateTime lastProfileUpdate;
    
    // Privacy settings
    @Column(nullable = false)
    private Boolean allowDataSharing = false;
    
    @Column(nullable = false)
    private Boolean allowThirdPartyMarketing = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime deletedAt; // Soft delete
    
    public enum Gender {
        MALE,
        FEMALE,
        OTHER,
        PREFER_NOT_TO_SAY
    }
    
    public enum VerificationStatus {
        PENDING,
        UNDER_REVIEW,
        VERIFIED,
        REJECTED,
        REQUIRES_UPDATE
    }
    
    public enum IdType {
        SSN,
        PASSPORT,
        DRIVERS_LICENSE,
        NATIONAL_ID,
        TAX_ID
    }
    
    public enum ProfileStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DELETED
    }
    
    // Constructors
    public UserProfile() {}
    
    public UserProfile(String userId, String fullName, String phoneNumber, LocalDate dateOfBirth) {
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        updateProfileCompletion();
    }
    
    // Business methods
    public void updateProfileCompletion() {
        int completedFields = 0;
        int totalFields = 15; // Adjust based on required fields
        
        if (fullName != null && !fullName.trim().isEmpty()) completedFields++;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) completedFields++;
        if (dateOfBirth != null) completedFields++;
        if (gender != null) completedFields++;
        if (occupation != null && !occupation.trim().isEmpty()) completedFields++;
        if (address != null && !address.trim().isEmpty()) completedFields++;
        if (city != null && !city.trim().isEmpty()) completedFields++;
        if (state != null && !state.trim().isEmpty()) completedFields++;
        if (postalCode != null && !postalCode.trim().isEmpty()) completedFields++;
        if (country != null && !country.trim().isEmpty()) completedFields++;
        if (nationalId != null && !nationalId.trim().isEmpty()) completedFields++;
        if (nationalIdType != null) completedFields++;
        if (employer != null && !employer.trim().isEmpty()) completedFields++;
        if (alternatePhoneNumber != null && !alternatePhoneNumber.trim().isEmpty()) completedFields++;
        if (nationalIdExpiryDate != null) completedFields++;
        
        this.profileCompletionPercentage = (completedFields * 100) / totalFields;
        this.lastProfileUpdate = LocalDateTime.now();
    }
    
    public boolean isProfileComplete() {
        return profileCompletionPercentage >= 80; // 80% completion threshold
    }
    
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }
    
    public boolean isActive() {
        return profileStatus == ProfileStatus.ACTIVE && deletedAt == null;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { 
        this.fullName = fullName;
        updateProfileCompletion();
    }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { 
        this.phoneNumber = phoneNumber;
        updateProfileCompletion();
    }
    
    public String getAlternatePhoneNumber() { return alternatePhoneNumber; }
    public void setAlternatePhoneNumber(String alternatePhoneNumber) { this.alternatePhoneNumber = alternatePhoneNumber; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { 
        this.dateOfBirth = dateOfBirth;
        updateProfileCompletion();
    }
    
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    
    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    
    public IdType getNationalIdType() { return nationalIdType; }
    public void setNationalIdType(IdType nationalIdType) { this.nationalIdType = nationalIdType; }
    
    public LocalDate getNationalIdExpiryDate() { return nationalIdExpiryDate; }
    public void setNationalIdExpiryDate(LocalDate nationalIdExpiryDate) { this.nationalIdExpiryDate = nationalIdExpiryDate; }
    
    public ProfileStatus getProfileStatus() { return profileStatus; }
    public void setProfileStatus(ProfileStatus profileStatus) { this.profileStatus = profileStatus; }
    
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public Boolean getMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(Boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }
    
    public Boolean getSmsNotificationEnabled() { return smsNotificationEnabled; }
    public void setSmsNotificationEnabled(Boolean smsNotificationEnabled) { this.smsNotificationEnabled = smsNotificationEnabled; }
    
    public Boolean getEmailNotificationEnabled() { return emailNotificationEnabled; }
    public void setEmailNotificationEnabled(Boolean emailNotificationEnabled) { this.emailNotificationEnabled = emailNotificationEnabled; }
    
    public Integer getProfileCompletionPercentage() { return profileCompletionPercentage; }
    public void setProfileCompletionPercentage(Integer profileCompletionPercentage) { this.profileCompletionPercentage = profileCompletionPercentage; }
    
    public LocalDateTime getLastProfileUpdate() { return lastProfileUpdate; }
    public void setLastProfileUpdate(LocalDateTime lastProfileUpdate) { this.lastProfileUpdate = lastProfileUpdate; }
    
    public Boolean getAllowDataSharing() { return allowDataSharing; }
    public void setAllowDataSharing(Boolean allowDataSharing) { this.allowDataSharing = allowDataSharing; }
    
    public Boolean getAllowThirdPartyMarketing() { return allowThirdPartyMarketing; }
    public void setAllowThirdPartyMarketing(Boolean allowThirdPartyMarketing) { this.allowThirdPartyMarketing = allowThirdPartyMarketing; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
