package com.fintech.userservice.controller;

import com.fintech.userservice.dto.request.UpdateUserRequest;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.security.annotation.FilterResponse;
import com.fintech.userservice.security.annotation.RequireAuthorization;
import com.fintech.userservice.security.service.AuthorizationService;
import com.fintech.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthorizationService authorizationService;

    public UserController(UserService userService,
                          AuthorizationService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }


    /**
     * Get current user's own profile (no userId parameter needed)
     */
    @GetMapping("/profile/me")
    @RequireAuthorization(message = "Access denied: Authentication required", resourceType = "user")
    @FilterResponse(resourceType = "user")
    public ResponseEntity<?> getMyProfile() {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User getting own profile: {}", currentUserId);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        Optional<UserProfile> userProfile = userService.getUserProfile(currentUserId);

        if (userProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Filtering will be applied automatically by @FilterResponse annotation
        return ResponseEntity.ok(userProfile.get());
    }

    /**
     * Get filtered profile data as a map (useful for dynamic field access)
     */
    @GetMapping("/profile/{userId}/filtered")
    @RequireAuthorization(
            message = "Access denied: You can only view your own profile data",
            resourceType = "user"
    )
    @FilterResponse(resourceType = "user", convertToMap = true)
    public ResponseEntity<?> getFilteredProfileData(@PathVariable String userId) {
        logger.info("Getting filtered profile data for userId: {} by user: {}",
                userId, authorizationService.getCurrentUserId());

        Optional<UserProfile> userProfile = userService.getUserProfile(userId);

        if (userProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Filtering will be applied automatically and converted to map
        return ResponseEntity.ok(userProfile.get());
    }

    /**
     * Search users by name, phone, email, or account number
     * Admins can search all users, regular users get limited access
     */
    @GetMapping("/search")
    @RequireAuthorization(message = "Access denied: Authentication required to search users", resourceType = "user")
    @FilterResponse(resourceType = "user")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        logger.info("Searching users with query: {} by user: {}", query, authorizationService.getCurrentUserId());

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid search query", "reason", "Search query cannot be empty"));
        }

        try {
            List<UserProfile> searchResults = userService.searchUsers(query);

            logger.info("Found {} users matching query: {}", searchResults.size(), query);

            // Return search results - filtering will be applied automatically
            return ResponseEntity.ok(Map.of(
                    "query", query,
                    "totalResults", searchResults.size(),
                    "results", searchResults
            ));

        } catch (Exception e) {
            logger.error("Error searching users with query: {}", query, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Search failed", "reason", e.getMessage()));
        }
    }


    @PutMapping("/role/{userId}")
    @RequireAuthorization(
            expression = "hasFullAccess()",
            message = "Access denied: Full access privileges required to update user roles",
            resourceType = "user"
    )
    public ResponseEntity<?> updateUserRole(@PathVariable String userId,
                                            @RequestBody Map<String, String> roleRequest) {
        logger.info("Updating role for userId: {} by admin: {}", userId, authorizationService.getCurrentUserId());

        String newRole = roleRequest.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid role", "reason", "Role cannot be empty"));
        }

        try {
            UserProfile updatedProfile = userService.changeUserRole(userId, newRole);

            logger.info("User role updated successfully for userId: {} to role: {}", userId, newRole);

            return ResponseEntity.ok(Map.of(
                    "message", "Role updated successfully",
                    "userId", userId,
                    "newRole", newRole,
                    "updatedAt", updatedProfile.getUpdatedAt(),
                    "updatedBy", authorizationService.getCurrentUserId()
            ));

        } catch (RuntimeException e) {
            logger.error("Error updating role for userId: {} to role: {}", userId, newRole, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Role update failed", "reason", e.getMessage()));
        }
    }

    /**
     * Health check endpoint (public)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "user-service",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
