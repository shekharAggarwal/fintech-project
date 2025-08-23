package com.fintech.userservice.controller;

import com.fintech.userservice.dto.AuthorizationResponse;
import com.fintech.userservice.dto.RoleChangeRequest;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.service.AuthorizationClientService;
import com.fintech.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Secure User Controller with Role-Based Authorization
 * <p>
 * All endpoints require proper authorization and check permissions in real-time.
 * Handles role changes dynamically and invalidates caches as needed.
 */
@RestController
//@RequestMapping("/api/user")
public class SecureUserController {
/*
    private static final Logger logger = LoggerFactory.getLogger(SecureUserController.class);

    final private UserService userService;

    final private AuthorizationClientService authorizationService;

    public SecureUserController(UserService userService, AuthorizationClientService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    *//**
     * Get user profile with proper authorization
     * - Users can view their own profile
     * - Admins can view any profile
     *//*
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Getting user profile for userId: {}", userId);

        // 1. Extract and validate token
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Missing or invalid authorization header"));
        }

        // 2. Check basic permission to view user profiles
        AuthorizationResponse authResponse = authorizationService.checkPermissionWithFreshData(
                token, "USER_PROFILE", "VIEW");

        if (!authResponse.isAuthorized()) {
            logger.warn("User denied access to view profile: {}", authResponse.getReason());
            return ResponseEntity.status(403).body(createErrorResponse(authResponse.getReason()));
        }

        // 3. Get the target user profile
        Optional<UserProfile> targetProfile = userService.getUserProfile(userId);
        if (targetProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 4. Check if user can view this specific profile
        String requestingUserEmail = authResponse.getUserEmail();
        UserProfile profile = targetProfile.get();

        // Allow if viewing own profile
        if (profile.getUserId().equals(requestingUserEmail)) {
            logger.info("User {} viewing own profile", requestingUserEmail);
            return ResponseEntity.ok(profile);
        }

        // Check admin permission for viewing other profiles
        AuthorizationResponse adminCheck = authorizationService.checkPermissionWithFreshData(
                token, "USER_PROFILE", "VIEW_ALL");

        if (!adminCheck.isAuthorized()) {
            logger.warn("User {} denied access to view other user's profile {}",
                    requestingUserEmail, userId);
            return ResponseEntity.status(403)
                    .body(createErrorResponse("You can only view your own profile or need admin privileges"));
        }

        logger.info("Admin {} viewing profile of user {}", requestingUserEmail, userId);
        return ResponseEntity.ok(profile);
    }

    *//**
     * Get user profile by account number with authorization
     *//*
    @GetMapping("/profile/account/{accountNumber}")
    public ResponseEntity<?> getUserProfileByAccountNumber(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Getting user profile by account number: {}", accountNumber);

        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Missing authorization header"));
        }

        // Check permission to search by account number (admin only)
        AuthorizationResponse authResponse = authorizationService.checkPermissionWithFreshData(
                token, "USER_ACCOUNT", "SEARCH");

        if (!authResponse.isAuthorized()) {
            logger.warn("User denied access to search by account number: {}", authResponse.getReason());
            return ResponseEntity.status(403).body(createErrorResponse(authResponse.getReason()));
        }

        Optional<UserProfile> userProfile = userService.getUserProfileByAccountNumber(accountNumber);

        if (userProfile.isPresent()) {
            logger.info("Found user profile for account number: {}", accountNumber);
            return ResponseEntity.ok(userProfile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    *//**
     * Update user profile with authorization
     *//*
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UserProfile updatedProfile,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Updating user profile for userId: {}", userId);

        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Missing authorization header"));
        }

        // Check update permission
        AuthorizationResponse authResponse = authorizationService.checkPermissionWithFreshData(
                token, "USER_PROFILE", "UPDATE");

        if (!authResponse.isAuthorized()) {
            logger.warn("User denied access to update profile: {}", authResponse.getReason());
            return ResponseEntity.status(403).body(createErrorResponse(authResponse.getReason()));
        }

        // Additional check: ensure user can only update their own profile unless admin
        String requestingUserEmail = authResponse.getUserEmail();
        Optional<UserProfile> existingProfile = userService.getUserProfile(userId);

        if (existingProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Allow if updating own profile
        if (!existingProfile.get().getUserId().equals(requestingUserEmail)) {
            // Check admin permission for updating other profiles
            AuthorizationResponse adminCheck = authorizationService.checkPermissionWithFreshData(
                    token, "USER_PROFILE", "UPDATE_ALL");

            if (!adminCheck.isAuthorized()) {
                logger.warn("User {} denied access to update other user's profile {}",
                        requestingUserEmail, userId);
                return ResponseEntity.status(403)
                        .body(createErrorResponse("You can only update your own profile or need admin privileges"));
            }
        }

        try {
            UserProfile updated = userService.updateUserProfile(userId, updatedProfile);
            logger.info("Successfully updated user profile for userId: {}", userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Failed to update user profile for userId: {}", userId, e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update profile: " + e.getMessage()));
        }
    }

    *//**
     * Admin endpoint to change user roles
     * This is where role changes happen - triggers cache invalidation
     *//*
    @PutMapping("/profile/{userId}/role")
    public ResponseEntity<?> changeUserRole(
            @PathVariable String userId,
            @RequestBody RoleChangeRequest request,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Changing role for userId: {} from {} to {}", userId, request.getOldRole(), request.getNewRole());

        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Missing authorization header"));
        }

        // Check admin permission for role changes
        AuthorizationResponse authResponse = authorizationService.checkPermissionWithFreshData(
                token, "USER_ROLE", "CHANGE");

        if (!authResponse.isAuthorized()) {
            logger.warn("User denied access to change roles: {}", authResponse.getReason());
            return ResponseEntity.status(403).body(createErrorResponse("Insufficient privileges to change user roles"));
        }

        try {
            // Get current user profile
            Optional<UserProfile> currentProfile = userService.getUserProfile(userId);
            if (currentProfile.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String currentRole = currentProfile.get().getRole();

            // Validate role change request
            if (!currentRole.equals(request.getOldRole())) {
                logger.warn("Role change request has incorrect old role. Current: {}, Requested: {}",
                        currentRole, request.getOldRole());
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Current role doesn't match the old role in request"));
            }

            // Update user role
            UserProfile userProfile = userService.changeUserRole(userId, request.getNewRole());

            // **CRITICAL: Notify authorization service about role change**
            // This will invalidate caches and update permissions immediately
            authorizationService.notifyRoleChange(
                    userProfile.getUserId(),
                    request.getOldRole(),
                    request.getNewRole(),
                    authResponse.getUserEmail() // Admin who made the change
            );

            logger.info("Successfully changed role for user {} from {} to {} by admin {}",
                    userProfile.getUserId(), request.getOldRole(), request.getNewRole(),
                    authResponse.getUserEmail());

            return ResponseEntity.ok(userProfile);

        } catch (Exception e) {
            logger.error("Failed to change user role for userId: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to change user role: " + e.getMessage()));
        }
    }

    *//**
     * Get all users (admin only)
     *//*
    @GetMapping("/profiles")
    public ResponseEntity<?> getAllUserProfiles(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Missing authorization header"));
        }

        // Check admin permission
        AuthorizationResponse authResponse = authorizationService.checkPermissionWithFreshData(
                token, "USER_PROFILE", "LIST_ALL");

        if (!authResponse.isAuthorized()) {
            logger.warn("User denied access to list all profiles: {}", authResponse.getReason());
            return ResponseEntity.status(403).body(createErrorResponse(authResponse.getReason()));
        }

        try {
            var profiles = userService.getAllUserProfiles(page, size);
            logger.info("Admin {} retrieved {} user profiles", authResponse.getUserEmail(), profiles.getTotalElements());
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            logger.error("Failed to get all user profiles", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to retrieve profiles"));
        }
    }

    *//**
     * Health check endpoint (no authorization required)
     *//*
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User service is healthy");
    }

    // Helper methods

    *//**
     * Extract JWT token from Authorization header
     *//*
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    *//**
     * Create standardized error response
     *//*
    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse("ACCESS_DENIED", message, System.currentTimeMillis());
    }

    *//**
     * Error response DTO
     *//*
    public static class ErrorResponse {
        private String error;
        private String message;
        private long timestamp;

        public ErrorResponse(String error, String message, long timestamp) {
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }*/
}
