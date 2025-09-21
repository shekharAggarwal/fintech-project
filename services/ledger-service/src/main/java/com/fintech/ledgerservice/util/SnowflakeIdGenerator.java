package com.fintech.ledgerservice.util;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance Snowflake ID generator for distributed systems
 * Generates 64-bit unique IDs with timestamp, machine ID, and sequence
 * Capable of generating 4.1M IDs per second per node
 */
@Component
public class SnowflakeIdGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdGenerator.class);
    
    // Snowflake algorithm constants
    private static final long EPOCH = 1640995200000L; // 2022-01-01 00:00:00 UTC
    
    // Bit allocation
    private static final long SEQUENCE_BITS = 12L;
    private static final long MACHINE_ID_BITS = 10L;
    private static final long TIMESTAMP_BITS = 41L;
    
    // Maximum values
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095
    
    // Bit shifts
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    
    @Value("${snowflake.machine-id:1}")
    private long machineId;
    
    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;
    
    // Performance optimization: pre-allocate sequence blocks
    private static final int SEQUENCE_BLOCK_SIZE = 100;
    private volatile long sequenceBlockStart = 0L;
    private volatile long sequenceBlockEnd = 0L;
    
    @PostConstruct
    public void init() {
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException(
                String.format("Machine ID must be between 0 and %d, got: %d", MAX_MACHINE_ID, machineId));
        }
        logger.info("Snowflake ID Generator initialized with machine ID: {}", machineId);
        logger.info("Theoretical max IDs per second: {} per node", MAX_SEQUENCE + 1);
    }
    
    /**
     * Generate next unique ID
     * Thread-safe implementation with performance optimizations
     */
    public synchronized String nextId() {
        long timestamp = getCurrentTimestamp();
        
        // Handle clock going backwards
        if (timestamp < lastTimestamp) {
            long clockDrift = lastTimestamp - timestamp;
            if (clockDrift > 5000) { // More than 5 seconds
                throw new RuntimeException(
                    String.format("Clock moved backwards by %d ms. Refusing to generate ID", clockDrift));
            }
            // Wait for clock to catch up for small drifts
            timestamp = waitForNextMillis(lastTimestamp);
        }
        
        // Same millisecond - increment sequence
        if (timestamp == lastTimestamp) {
            // Use sequence block allocation for better performance
            if (sequence >= sequenceBlockEnd) {
                allocateSequenceBlock();
            }
            sequence++;
            
            // Sequence overflow - wait for next millisecond
            if (sequence > MAX_SEQUENCE) {
                timestamp = waitForNextMillis(lastTimestamp);
                sequence = 0L;
                allocateSequenceBlock();
            }
        } else {
            // New millisecond - reset sequence
            sequence = 0L;
            allocateSequenceBlock();
        }
        
        lastTimestamp = timestamp;
        
        // Construct the ID
        long id = ((timestamp - EPOCH) << TIMESTAMP_SHIFT) |
                  (machineId << MACHINE_ID_SHIFT) |
                  sequence;
        
        return String.valueOf(id);
    }
    
    /**
     * Pre-allocate sequence block for better performance
     */
    private void allocateSequenceBlock() {
        sequenceBlockStart = sequence;
        sequenceBlockEnd = Math.min(sequence + SEQUENCE_BLOCK_SIZE, MAX_SEQUENCE);
    }
    
    /**
     * Wait for next millisecond
     */
    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
    
    /**
     * Get current timestamp
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * Parse timestamp from ID
     */
    public long getTimestampFromId(String id) {
        try {
            long longId = Long.parseLong(id);
            return ((longId >> TIMESTAMP_SHIFT) + EPOCH);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Snowflake ID format: " + id, e);
        }
    }
    
    /**
     * Parse machine ID from ID
     */
    public long getMachineIdFromId(String id) {
        try {
            long longId = Long.parseLong(id);
            return (longId >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Snowflake ID format: " + id, e);
        }
    }
    
    /**
     * Parse sequence from ID
     */
    public long getSequenceFromId(String id) {
        try {
            long longId = Long.parseLong(id);
            return longId & MAX_SEQUENCE;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Snowflake ID format: " + id, e);
        }
    }
    
    /**
     * Get current configuration info
     */
    public String getInfo() {
        return String.format(
            "SnowflakeIdGenerator[machineId=%d, epoch=%d, maxIdsPerSecond=%d]",
            machineId, EPOCH, MAX_SEQUENCE + 1
        );
    }
}