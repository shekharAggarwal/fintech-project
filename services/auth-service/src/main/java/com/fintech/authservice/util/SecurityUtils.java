package com.fintech.authservice.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Security utility class for password hashing, token generation, and security operations
 */
public class SecurityUtils {

    private static final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private static final SecureRandom secureRandom = new SecureRandom();

    // Password strength patterns
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    /**
     * Generate a cryptographically secure salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash password with salt using BCrypt
     */
    public static String hashPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return passwordEncoder.encode(saltedPassword);
    }

    /**
     * Verify password against hash with salt
     */
    public static boolean verifyPassword(String password, String hash, String salt) {
        String saltedPassword = password + salt;
        return passwordEncoder.matches(saltedPassword, hash);
    }

    /**
     * Generate a secure token for email verification, password reset, etc.
     */
    public static String generateSecureToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Calculate password strength (0-100)
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        // Character variety scoring
        if (UPPERCASE.matcher(password).find()) score += 15;
        if (LOWERCASE.matcher(password).find()) score += 15;
        if (DIGITS.matcher(password).find()) score += 15;
        if (SPECIAL.matcher(password).find()) score += 15;

        // Penalty for common patterns
        if (password.toLowerCase().contains("password")) score -= 20;
        if (password.toLowerCase().contains("123456")) score -= 20;
        if (password.toLowerCase().matches(".*(..).*\\1.*")) score -= 10; // Repeated patterns

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Check if password meets minimum security requirements
     */
    public static boolean isPasswordSecure(String password) {
        return calculatePasswordStrength(password) >= 60;
    }
    
    /**
     * Generate a cryptographically secure session ID for production use
     */
    public static String generateCryptographicallySecureSessionId() {
        byte[] sessionId = new byte[32]; // 256 bits
        secureRandom.nextBytes(sessionId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sessionId);
    }
    
    /**
     * Constant time delay to prevent timing attacks
     */
    public static void constantTimeDelay() {
        try {
            Thread.sleep(100 + secureRandom.nextInt(50)); // 100-150ms random delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate device fingerprint based on user agent and other factors
     */
    public static String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = userAgent + "|" + ipAddress;
        return Base64.getEncoder().encodeToString(combined.getBytes());
    }

    /**
     * Sanitize input to prevent injection attacks
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;

        return input.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }

    /**
     * Check if IP address is in a valid format
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        // IPv4 pattern
        String ipv4Pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";
        // IPv6 pattern (simplified)
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";

        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "Unknown";
    }
}
