package com.fintech.userservice.service;

import com.fintech.userservice.dto.message.UserCreationMessage;
import com.fintech.userservice.dto.message.UserGreetingNotification;
import com.fintech.userservice.dto.message.UserRoleRegistrationMessage;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.messaging.EmailNotificationPublisher;
import com.fintech.userservice.messaging.AuthorizationKafkaPublisher;
import com.fintech.userservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    final private UserProfileRepository userProfileRepository;
    final private EmailNotificationPublisher emailNotificationPublisher;
    final private AuthorizationKafkaPublisher authorizationKafkaPublisher;

    public UserService(UserProfileRepository userProfileRepository, 
                      EmailNotificationPublisher emailNotificationPublisher,
                      AuthorizationKafkaPublisher authorizationKafkaPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.emailNotificationPublisher = emailNotificationPublisher;
        this.authorizationKafkaPublisher = authorizationKafkaPublisher;
    }

    /**
     * Create user profile from message received via Kafka
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
                message.getEmail(),
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
            
            // Send user role registration to authorization service via Kafka
            try {
                UserRoleRegistrationMessage roleMessage = new UserRoleRegistrationMessage(
                    message.getUserId(),
                    message.getRole(),
                    System.currentTimeMillis()
                );
                
                authorizationKafkaPublisher.publishUserRoleRegistration(roleMessage);
                logger.info("Published user role registration to authorization service for userId: {} with role: {}", 
                           message.getUserId(), message.getRole());
            } catch (Exception e) {
                logger.error("Failed to publish user role registration for userId: {} with role: {}", 
                           message.getUserId(), message.getRole(), e);
                // Note: We don't fail the user creation if role registration fails
                // The role can be registered later through manual process or retry mechanism
            }
            
            // Send greeting email notification via RabbitMQ
            try {
                UserGreetingNotification greetingNotification = new UserGreetingNotification(
                    message.getEmail(),
                    message.getFullName(),
                    message.getUserId(),
                    accountNumber,
                    System.currentTimeMillis()
                );
                
                emailNotificationPublisher.publishUserGreetingEmail(greetingNotification);
                logger.info("Published user greeting email notification for userId: {} and email: {}", 
                           message.getUserId(), message.getEmail());
            } catch (Exception e) {
                logger.error("Failed to publish user greeting email notification for userId: {}", 
                           message.getUserId(), e);
                // Note: We don't fail the user creation if email notification fails
                // The user profile is already created and email can be sent later
            }
            
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
     * Update user profile using UpdateUserRequest DTO
     */
    public UserProfile updateUserProfile(String userId, com.fintech.userservice.dto.request.UpdateUserRequest updateRequest) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);
        
        if (existingProfile.isEmpty()) {
            throw new RuntimeException("User profile not found for userId: " + userId);
        }
        
        UserProfile profile = existingProfile.get();
        
        // Update only the fields that are provided in the request
        if (updateRequest.getFirstName() != null || updateRequest.getLastName() != null) {
            String fullName = "";
            if (updateRequest.getFirstName() != null && updateRequest.getLastName() != null) {
                fullName = updateRequest.getFirstName() + " " + updateRequest.getLastName();
            } else if (updateRequest.getFirstName() != null) {
                // Keep existing last name if only first name is provided
                String[] nameParts = profile.getFullName() != null ? profile.getFullName().split(" ", 2) : new String[]{"", ""};
                fullName = updateRequest.getFirstName() + (nameParts.length > 1 ? " " + nameParts[1] : "");
            } else {
                // Keep existing first name if only last name is provided
                String[] nameParts = profile.getFullName() != null ? profile.getFullName().split(" ", 2) : new String[]{"", ""};
                fullName = (nameParts.length > 0 ? nameParts[0] : "") + " " + updateRequest.getLastName();
            }
            profile.setFullName(fullName.trim());
        }
        
        if (updateRequest.getEmail() != null) {
            profile.setEmail(updateRequest.getEmail());
        }
        
        if (updateRequest.getPhoneNumber() != null) {
            profile.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        
        if (updateRequest.getAddress() != null) {
            profile.setAddress(updateRequest.getAddress());
        }
        
        logger.info("Updating user profile for userId: {}", userId);
        return userProfileRepository.save(profile);
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
    
    /**
     * Change user role (admin function)
     */
    public UserProfile changeUserRole(String userId, String newRole) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);
        
        if (existingProfile.isPresent()) {
            UserProfile profile = existingProfile.get();
            String oldRole = profile.getRole();
            profile.setRole(newRole);
            
            UserProfile updatedProfile = userProfileRepository.save(profile);
            
            logger.info("Changed role for user {} from {} to {}", userId, oldRole, newRole);
            return updatedProfile;
        } else {
            throw new RuntimeException("User profile not found for userId: " + userId);
        }
    }
    
    /**
     * Get all user profiles with pagination (admin function)
     */
    public Page<UserProfile> getAllUserProfiles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userProfileRepository.findAll(pageable);
    }
    
    /**
     * Get all user profiles as list (admin function)
     */
    public List<UserProfile> getAllUserProfilesList() {
        return userProfileRepository.findAll();
    }
    
    /**
     * Search users by name, phone, email, or account number
     */
    public List<UserProfile> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of(); // Return empty list for empty search term
        }
        
        logger.info("Searching users with term: {}", searchTerm);
        return userProfileRepository.searchUsers(searchTerm.trim());
    }
}
