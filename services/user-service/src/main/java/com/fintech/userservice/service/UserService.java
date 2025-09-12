package com.fintech.userservice.service;

import com.fintech.userservice.dto.message.UserCreationMessage;
import com.fintech.userservice.dto.message.UserGreetingNotification;
import com.fintech.userservice.dto.message.UserRoleRegistrationMessage;
import com.fintech.userservice.dto.request.UpdateUserRequest;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.messaging.AuthorizationKafkaPublisher;
import com.fintech.userservice.messaging.EmailNotificationPublisher;
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

    final private AuthorizationServiceClient authorizationServiceClient;

    public UserService(UserProfileRepository userProfileRepository,
                       EmailNotificationPublisher emailNotificationPublisher,
                       AuthorizationKafkaPublisher authorizationKafkaPublisher,
                       AuthorizationServiceClient authorizationServiceClient) {
        this.userProfileRepository = userProfileRepository;
        this.emailNotificationPublisher = emailNotificationPublisher;
        this.authorizationKafkaPublisher = authorizationKafkaPublisher;
        this.authorizationServiceClient = authorizationServiceClient;
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
     * Update user profile using UpdateUserRequest DTO
     */
    public UserProfile updateUserProfileFromRequest(String userId, UpdateUserRequest updateRequest) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);

        if (existingProfile.isEmpty()) {
            throw new RuntimeException("User profile not found for userId: " + userId);
        }

        UserProfile profile = existingProfile.get();

        // Update full name if first name or last name is provided
        if (updateRequest.getFirstName() != null || updateRequest.getLastName() != null) {
            String fullName = buildFullName(updateRequest, profile.getFullName());
            profile.setFullName(fullName);
        }

        // Update other fields if provided
        if (updateRequest.getPhoneNumber() != null) {
            profile.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getAddress() != null) {
            profile.setAddress(updateRequest.getAddress());
        }

        UserProfile updatedProfile = userProfileRepository.save(profile);
        logger.info("User profile updated for userId: {}", userId);

        return updatedProfile;
    }

    /**
     * Helper method to build full name from UpdateUserRequest
     */
    private String buildFullName(UpdateUserRequest updateRequest, String currentFullName) {
        if (updateRequest.getFirstName() != null && updateRequest.getLastName() != null) {
            return updateRequest.getFirstName() + " " + updateRequest.getLastName();
        } else {
            // Keep existing name parts if only one is provided
            String[] currentNameParts = currentFullName.split(" ", 2);
            String firstName = updateRequest.getFirstName() != null ? updateRequest.getFirstName() :
                    (currentNameParts.length > 0 ? currentNameParts[0] : "");
            String lastName = updateRequest.getLastName() != null ? updateRequest.getLastName() :
                    (currentNameParts.length > 1 ? currentNameParts[1] : "");
            return (firstName + " " + lastName).trim();
        }
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

    /**
     * Change user role (admin function)
     * This method updates both user profile and authorization service synchronously
     */
    public UserProfile changeUserRole(String userId, String newRole, String updatedBy) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);

        if (existingProfile.isEmpty()) {
            throw new RuntimeException("User profile not found for userId: " + userId);
        }

        UserProfile profile = existingProfile.get();
        String oldRole = profile.getRole();

        logger.info("Changing role for user {} from {} to {} by {}", userId, oldRole, newRole, updatedBy);

        try {
            // First, update the authorization service synchronously
            // This ensures authorization service is updated before user profile
            authorizationServiceClient.updateUserRole(userId, newRole, updatedBy);

            // If authorization service update succeeds, update user profile
            profile.setRole(newRole);
            UserProfile updatedProfile = userProfileRepository.save(profile);

            logger.info("Successfully changed role for user {} from {} to {} by {}",
                    userId, oldRole, newRole, updatedBy);
            return updatedProfile;

        } catch (Exception e) {
            logger.error("Failed to change role for user {} from {} to {} by {}: {}",
                    userId, oldRole, newRole, updatedBy, e.getMessage(), e);
            throw new RuntimeException("Failed to update user role: " + e.getMessage(), e);
        }
    }

    /**
     * Change user role (admin function) - backward compatibility
     */
    public UserProfile changeUserRole(String userId, String newRole) {
        return changeUserRole(userId, newRole, "system");
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
