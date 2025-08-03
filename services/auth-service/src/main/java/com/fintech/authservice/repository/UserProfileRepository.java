package com.fintech.authservice.repository;

import com.fintech.authservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    Optional<UserProfile> findByUserId(String userId);
    
    @Query("SELECT p FROM UserProfile p WHERE p.userId = :userId AND p.profileStatus = 'ACTIVE' AND p.deletedAt IS NULL")
    Optional<UserProfile> findActiveProfileByUserId(@Param("userId") String userId);
    
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByNationalId(String nationalId);
    
    @Query("SELECT p FROM UserProfile p WHERE p.verificationStatus = :status")
    List<UserProfile> findByVerificationStatus(@Param("status") UserProfile.VerificationStatus status);
    
    @Query("SELECT p FROM UserProfile p WHERE p.profileCompletionPercentage < :threshold AND p.profileStatus = 'ACTIVE'")
    List<UserProfile> findIncompleteProfiles(@Param("threshold") Integer threshold);
    
    @Query("SELECT p FROM UserProfile p WHERE p.lastProfileUpdate < :cutoffDate AND p.profileStatus = 'ACTIVE'")
    List<UserProfile> findStaleProfiles(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.verificationStatus = :status")
    long countByVerificationStatus(@Param("status") UserProfile.VerificationStatus status);
    
    @Query("SELECT AVG(p.profileCompletionPercentage) FROM UserProfile p WHERE p.profileStatus = 'ACTIVE'")
    Double getAverageProfileCompletion();
    
    @Query("SELECT p FROM UserProfile p WHERE p.nationalIdExpiryDate < :expiryDate AND p.verificationStatus = 'VERIFIED'")
    List<UserProfile> findProfilesWithExpiringIds(@Param("expiryDate") LocalDateTime expiryDate);
    
    @Query("SELECT p FROM UserProfile p WHERE p.country = :country AND p.profileStatus = 'ACTIVE'")
    List<UserProfile> findByCountry(@Param("country") String country);
    
    @Query("SELECT p FROM UserProfile p WHERE p.marketingOptIn = true AND p.emailNotificationEnabled = true")
    List<UserProfile> findMarketingOptInProfiles();
}
