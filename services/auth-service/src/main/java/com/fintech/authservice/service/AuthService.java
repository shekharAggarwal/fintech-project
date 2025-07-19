package com.fintech.authservice.service;

import com.fintech.authservice.dto.*;
import com.fintech.authservice.entity.User;
import com.fintech.authservice.repository.UserRepository;
import com.fintech.authservice.util.JwtUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final AuditService auditService;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_session:";

    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtUtil jwtUtil, 
                      RedisTemplate<String, Object> redisTemplate, 
                      RabbitTemplate rabbitTemplate,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.auditService = auditService;
    }

    public AuthResponse register(RegistrationRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                auditService.logAuthEvent(
                    AuditService.AuditEventType.REGISTRATION_FAILED,
                    request.getEmail(),
                    getClientIpAddress(),
                    getUserAgent(),
                    false,
                    "Email already exists"
                );
                return new AuthResponse("User with this email already exists");
            }

            // Create new user
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPhoneNumber(request.getPhoneNumber());
            user.setAddress(request.getAddress());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setOccupation(request.getOccupation());
            user.setInitialDeposit(request.getInitialDeposit());
            user.setAccountBalance(request.getInitialDeposit());

            // Set user role
            try {
                user.setRole(User.UserRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException e) {
                user.setRole(User.UserRole.ACCOUNT_HOLDER); // Default to account holder
            }

            // Generate account number and user ID
            user.setAccountNumber(generateAccountNumber());
            user.setUserId(UUID.randomUUID().toString());

            // Calculate shard key for database sharding (handled by ShardingSphere-Proxy)
            user.setShardKey(user.getUserId().hashCode() % 3 + 1); // Simple hash for reference

            // Save user - ShardingSphere-Proxy will handle the sharding automatically
            User savedUser = userRepository.save(user);

            // Send welcome email via RabbitMQ
            sendWelcomeEmail(savedUser);

            // Log successful registration
            auditService.logAuthEvent(
                AuditService.AuditEventType.REGISTRATION_SUCCESS,
                savedUser.getUserId(),
                getClientIpAddress(),
                getUserAgent(),
                true,
                "User registered successfully"
            );

            return new AuthResponse("User registered successfully. Please check your email for account details.");
            
        } catch (Exception e) {
            auditService.logAuthEvent(
                AuditService.AuditEventType.REGISTRATION_FAILED,
                request.getEmail(),
                getClientIpAddress(),
                getUserAgent(),
                false,
                "Registration failed: " + e.getMessage()
            );
            throw new RuntimeException("Registration failed", e);
        }
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findActiveUserByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return new AuthResponse("Invalid email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse("Invalid email or password");
        }

        // Check if user already has an active session
        if (user.getCurrentSessionId() != null) {
            // Invalidate existing session
            invalidateSession(user.getCurrentSessionId());
        }

        // Generate new session ID
        String sessionId = UUID.randomUUID().toString();

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), sessionId);

        // Update user with session info
        user.setCurrentSessionId(sessionId);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiryDate(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Store session in Redis
        storeSession(sessionId, user.getEmail(), 10); // 10 minutes

        return new AuthResponse(accessToken, refreshToken, user.getEmail(),
                user.getFullName(), user.getAccountNumber(), user.getAccountBalance(),
                user.getRole().name());
    }

    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            return new AuthResponse("Invalid or expired refresh token");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        String sessionId = jwtUtil.getSessionIdFromToken(refreshToken);

        Optional<User> userOpt = userRepository.findActiveUserByEmail(email);

        if (userOpt.isEmpty() || !refreshToken.equals(userOpt.get().getRefreshToken())) {
            return new AuthResponse("Invalid refresh token");
        }

        User user = userOpt.get();

        // Check if session is still valid
        if (!isSessionValid(sessionId)) {
            return new AuthResponse("Session expired. Please login again.");
        }

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(email, sessionId);

        // Extend session
        extendSession(sessionId, 10); // Extend by 10 minutes

        return new AuthResponse(newAccessToken, refreshToken, user.getEmail(),
                user.getFullName(), user.getAccountNumber(), user.getAccountBalance(),
                user.getRole().name());
    }

    public AuthResponse logout(String email) {
        Optional<User> userOpt = userRepository.findActiveUserByEmail(email);

        if (userOpt.isEmpty()) {
            return new AuthResponse("User not found");
        }

        User user = userOpt.get();

        if (user.getCurrentSessionId() != null) {
            invalidateSession(user.getCurrentSessionId());
            user.setCurrentSessionId(null);
            user.setRefreshToken(null);
            user.setRefreshTokenExpiryDate(null);
            userRepository.save(user);
        }

        return new AuthResponse("Logged out successfully");
    }

    public AuthResponse changePassword(String email, ChangePasswordRequest request) {
        Optional<User> userOpt = userRepository.findActiveUserByEmail(email);

        if (userOpt.isEmpty()) {
            return new AuthResponse("User not found");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return new AuthResponse("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new AuthResponse("Password changed successfully");
    }

    public AuthResponse deleteAccount(String email) {
        Optional<User> userOpt = userRepository.findActiveUserByEmail(email);

        if (userOpt.isEmpty()) {
            return new AuthResponse("User not found");
        }

        User user = userOpt.get();

        // Soft delete - clear personal information but keep transaction history
        user.setFullName("DELETED USER");
        user.setEmail("deleted_" + System.currentTimeMillis() + "@deleted.com");
        user.setPhoneNumber("DELETED");
        user.setAddress("DELETED");
        user.setDateOfBirth("DELETED");
        user.setOccupation("DELETED");
        user.setIsActive(false);
        user.setAccountStatus(User.AccountStatus.CLOSED);

        // Invalidate session
        if (user.getCurrentSessionId() != null) {
            invalidateSession(user.getCurrentSessionId());
            user.setCurrentSessionId(null);
            user.setRefreshToken(null);
            user.setRefreshTokenExpiryDate(null);
        }

        userRepository.save(user);

        return new AuthResponse("Account closed successfully");
    }

    public boolean validateSession(String sessionId) {
        return isSessionValid(sessionId);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "ACC" + System.currentTimeMillis() +
                    String.valueOf((int) (Math.random() * 1000));
        } while (userRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private void storeSession(String sessionId, String email, int durationMinutes) {
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId, email,
                durationMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(USER_SESSION_PREFIX + email, sessionId,
                durationMinutes, TimeUnit.MINUTES);
    }

    private boolean isSessionValid(String sessionId) {
        return redisTemplate.hasKey(SESSION_PREFIX + sessionId);
    }

    private void extendSession(String sessionId, int durationMinutes) {
        String email = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (email != null) {
            redisTemplate.expire(SESSION_PREFIX + sessionId, durationMinutes, TimeUnit.MINUTES);
            redisTemplate.expire(USER_SESSION_PREFIX + email, durationMinutes, TimeUnit.MINUTES);
        }
    }

    private void invalidateSession(String sessionId) {
        String email = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (email != null) {
            redisTemplate.delete(SESSION_PREFIX + sessionId);
            redisTemplate.delete(USER_SESSION_PREFIX + email);
        }
    }

    private void sendWelcomeEmail(User user) {
        // Create email message for RabbitMQ
        var emailMessage = new Object() {
            @SuppressWarnings("unused")
            public final String to = user.getEmail();
            @SuppressWarnings("unused")
            public final String subject = "Welcome to FinTech Bank - Account Created Successfully";
            @SuppressWarnings("unused")
            public final String body = String.format(
                    "Dear %s,\n\n" +
                            "Welcome to FinTech Bank! Your account has been created successfully.\n\n" +
                            "Account Details:\n" +
                            "Account Number: %s\n" +
                            "Account Balance: $%.2f\n" +
                            "Email: %s\n\n" +
                            "You can now login to your account using your email and password.\n\n" +
                            "Thank you for choosing FinTech Bank!\n\n" +
                            "Best regards,\n" +
                            "FinTech Bank Team",
                    user.getFullName(), user.getAccountNumber(), user.getAccountBalance(), user.getEmail()
            );
            @SuppressWarnings("unused")
            public final String type = "WELCOME";
        };

        // Send to notification service via RabbitMQ
        rabbitTemplate.convertAndSend("notification.exchange", "notification.email", emailMessage);
    }
    
    // Helper methods for audit logging
    private String getClientIpAddress() {
        // This would typically come from the HTTP request context
        // For now, return a placeholder
        return "127.0.0.1";
    }
    
    private String getUserAgent() {
        // This would typically come from the HTTP request context
        // For now, return a placeholder
        return "Unknown";
    }
}
