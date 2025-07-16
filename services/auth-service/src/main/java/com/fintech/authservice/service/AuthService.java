package com.fintech.authservice.service;

import com.fintech.authservice.dto.*;
import com.fintech.authservice.entity.User;
import com.fintech.authservice.repository.UserRepository;
import com.fintech.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_session:";
    
    public AuthResponse register(RegistrationRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
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
        
        // Generate account number
        user.setAccountNumber(generateAccountNumber());
        
        // Calculate shard key for database sharding
        user.setShardKey(calculateShardKey(user.getAccountNumber()));
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Send welcome email via RabbitMQ
        sendWelcomeEmail(savedUser);
        
        return new AuthResponse("User registered successfully. Please check your email for account details.");
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
                           String.valueOf((int)(Math.random() * 1000));
        } while (userRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
    
    private Integer calculateShardKey(String accountNumber) {
        // Simple hash-based sharding - distribute across 4 shards
        return Math.abs(accountNumber.hashCode()) % 4;
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
            public final String to = user.getEmail();
            public final String subject = "Welcome to FinTech Bank - Account Created Successfully";
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
            public final String type = "WELCOME";
        };
        
        // Send to notification service via RabbitMQ
        rabbitTemplate.convertAndSend("notification.exchange", "notification.email", emailMessage);
    }
}
