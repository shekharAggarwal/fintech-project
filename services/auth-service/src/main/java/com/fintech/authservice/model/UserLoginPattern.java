package com.fintech.authservice.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User login pattern tracking for risk assessment
 */
public class UserLoginPattern {
    
    private final Set<String> knownIpAddresses = new HashSet<>();
    private final Set<String> knownCountries = new HashSet<>();
    private final Set<String> knownDevices = new HashSet<>();
    private final Set<String> knownBrowsers = new HashSet<>();
    private final Set<String> knownOperatingSystems = new HashSet<>();
    private final Set<Integer> typicalLoginHours = new HashSet<>();
    
    private final AtomicLong recentLoginCount = new AtomicLong(0);
    private LocalDateTime lastLoginTime;
    private LocalDateTime patternCreated;
    
    public UserLoginPattern() {
        this.patternCreated = LocalDateTime.now();
    }
    
    /**
     * Add a new login event to the pattern
     */
    public void addLoginEvent(String ipAddress, String userAgent, String deviceFingerprint, LocalDateTime loginTime) {
        // Add IP address
        knownIpAddresses.add(ipAddress);
        
        // Extract and add country (implement GeoIP lookup)
        String country = extractCountryFromIP(ipAddress);
        if (country != null) {
            knownCountries.add(country);
        }
        
        // Add device fingerprint
        knownDevices.add(deviceFingerprint);
        
        // Extract and add browser/OS information
        String browser = extractBrowser(userAgent);
        String os = extractOS(userAgent);
        
        if (browser != null) knownBrowsers.add(browser);
        if (os != null) knownOperatingSystems.add(os);
        
        // Add login hour
        typicalLoginHours.add(loginTime.getHour());
        
        // Update login tracking
        lastLoginTime = loginTime;
        
        // Increment recent login count
        recentLoginCount.incrementAndGet();
        
        // Cleanup old data (implement retention policy)
        cleanupOldData();
    }
    
    /**
     * Get recent login count (last 24 hours)
     */
    public long getRecentLoginCount() {
        // In a real implementation, this would count logins in the last 24 hours
        // For now, return the atomic counter value
        return recentLoginCount.get();
    }
    
    /**
     * Reset recent login count
     */
    public void resetRecentLoginCount() {
        recentLoginCount.set(0);
    }
    
    // Getters
    public Set<String> getKnownIpAddresses() { return new HashSet<>(knownIpAddresses); }
    public Set<String> getKnownCountries() { return new HashSet<>(knownCountries); }
    public Set<String> getKnownDevices() { return new HashSet<>(knownDevices); }
    public Set<String> getKnownBrowsers() { return new HashSet<>(knownBrowsers); }
    public Set<String> getKnownOperatingSystems() { return new HashSet<>(knownOperatingSystems); }
    public Set<Integer> getTypicalLoginHours() { return new HashSet<>(typicalLoginHours); }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public LocalDateTime getPatternCreated() { return patternCreated; }
    
    /**
     * Clean up old data to prevent unlimited growth
     */
    private void cleanupOldData() {
        // Limit the number of known IP addresses
        if (knownIpAddresses.size() > 50) {
            // Remove oldest entries (implement LRU cache logic)
            // For simplicity, we'll just keep the current implementation
        }
        
        // Limit device fingerprints
        if (knownDevices.size() > 10) {
            // Remove oldest device fingerprints
        }
    }
    
    /**
     * Extract country from IP address
     */
    private String extractCountryFromIP(String ipAddress) {
        // Implement GeoIP lookup
        // For now, return null (placeholder)
        return null;
    }
    
    /**
     * Extract browser from user agent
     */
    private String extractBrowser(String userAgent) {
        if (userAgent == null) return null;
        
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        return "Unknown";
    }
    
    /**
     * Extract operating system from user agent
     */
    private String extractOS(String userAgent) {
        if (userAgent == null) return null;
        
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS")) return "iOS";
        return "Unknown";
    }
}
