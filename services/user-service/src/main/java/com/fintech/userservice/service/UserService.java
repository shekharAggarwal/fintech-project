package com.fintech.userservice.service;

import com.fintech.userservice.dto.UserCreationMessage;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    final private UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Create user profile from message received via RabbitMQ
     */
    public void createUserProfile(UserCreationMessage message) {
        try {
            logger.info("Creating user profile for userId: {}", message.getUserId());
            
            // Check if user already exists
            if (userProfileRepository.existsByUserId(message.getUserId())) {
                logger.warn("User profile already exists for userId: {}", message.getUserId());
                return;
            }
            
            // Generate unique account number
            String accountNumber = generateAccountNumber();
            
            // Create user profile entity
            UserProfile userProfile = new UserProfile(
                message.getUserId(),
                message.getFullName(),
                message.getPhoneNumber(),
                message.getAddress(),
                message.getDateOfBirth(),
                message.getOccupation(),
                message.getInitialDeposit(),
                message.getRole()
            );
            
            userProfile.setAccountNumber(accountNumber);
            
            // Save to database
            userProfileRepository.save(userProfile);
            
            logger.info("User profile created successfully for userId: {} with account number: {}", 
                       message.getUserId(), accountNumber);
            
        } catch (Exception e) {
            logger.error("Failed to create user profile for userId: {}", message.getUserId(), e);
            throw new RuntimeException("Failed to create user profile", e);
        }
    }
    
    /**
     * Get user profile by userId
     */
    public Optional<UserProfile> getUserProfile(String userId) {
        return userProfileRepository.findByUserId(userId);
    }
    
    /**
     * Get user profile by account number
     */
    public Optional<UserProfile> getUserProfileByAccountNumber(String accountNumber) {
        return userProfileRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Update user profile
     */
    public UserProfile updateUserProfile(String userId, UserProfile updatedProfile) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);
        
        if (existingProfile.isPresent()) {
            UserProfile profile = existingProfile.get();
            profile.setFullName(updatedProfile.getFullName());
            profile.setPhoneNumber(updatedProfile.getPhoneNumber());
            profile.setAddress(updatedProfile.getAddress());
            profile.setOccupation(updatedProfile.getOccupation());
            
            return userProfileRepository.save(profile);
        } else {
            throw new RuntimeException("User profile not found for userId: " + userId);
        }
    }
    
    /**
     * Generate unique account number
     */
    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate 12-digit account number
            accountNumber = String.format("%012d", Math.abs(new Random().nextLong()) % 1000000000000L);
        } while (userProfileRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
}
