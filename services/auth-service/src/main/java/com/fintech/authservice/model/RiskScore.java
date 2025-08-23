package com.fintech.authservice.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Risk score calculation for authentication attempts
 */
public class RiskScore {
    
    private final Map<String, Integer> factors = new HashMap<>();
    private int totalScore = 0;
    
    public RiskScore() {}
    
    /**
     * Add a risk factor
     */
    public void addFactor(String factorName, int score) {
        factors.put(factorName, score);
        calculateTotalScore();
    }
    
    /**
     * Calculate total risk score
     */
    private void calculateTotalScore() {
        totalScore = factors.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get total risk score (0-100)
     */
    public int getScore() {
        return Math.min(totalScore, 100); // Cap at 100
    }
    
    /**
     * Check if this is a low risk score
     */
    public boolean isLowRisk() {
        return getScore() < 30;
    }
    
    /**
     * Check if this is a medium risk score
     */
    public boolean isMediumRisk() {
        return getScore() >= 30 && getScore() < 60;
    }
    
    /**
     * Check if this is a high risk score
     */
    public boolean isHighRisk() {
        return getScore() >= 60;
    }
    
    /**
     * Get risk level as string
     */
    public String getRiskLevel() {
        if (isLowRisk()) return "LOW";
        if (isMediumRisk()) return "MEDIUM";
        return "HIGH";
    }
    
    /**
     * Get individual risk factors
     */
    public Map<String, Integer> getFactors() {
        return new HashMap<>(factors);
    }
    
    @Override
    public String toString() {
        return String.format("RiskScore{total=%d, level=%s, factors=%s}", 
                           getScore(), getRiskLevel(), factors);
    }
}
