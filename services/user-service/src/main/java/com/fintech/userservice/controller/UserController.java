package com.fintech.userservice.controller;

import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    final private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get user profile by userId
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId) {
        Optional<UserProfile> userProfile = userService.getUserProfile(userId);

        return userProfile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get user profile by account number
     */
    @GetMapping("/profile/account/{accountNumber}")
    public ResponseEntity<UserProfile> getUserProfileByAccountNumber(@PathVariable String accountNumber) {
        Optional<UserProfile> userProfile = userService.getUserProfileByAccountNumber(accountNumber);

        return userProfile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> updateUserProfile(@PathVariable String userId,
                                                         @RequestBody UserProfile updatedProfile) {
        try {
            UserProfile updated = userService.updateUserProfile(userId, updatedProfile);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> test() {
        return  ResponseEntity.ok(new UserProfile());
    }
}
