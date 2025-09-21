package com.fintech.paymentservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * High-performance Snowflake ID generator for unique payment IDs
 * Enhanced version of Twitter's Snowflake algorithm for high-throughput scenarios
 * 
 * Bit allocation (64 bits total):
 * - 1 bit: Sign bit (always 0 for positive numbers)
 * - 41 bits: Timestamp (milliseconds since custom epoch) - ~69 years
 * - 10 bits: Node ID (supports up to 1024 nodes)
 * - 12 bits: Sequence number (4096 IDs per millisecond per node)
 * 
 * Total capacity: ~4 million IDs per second per node
 */
@Component
public class SnowflakeIdGenerator {
    
    // Custom epoch (January 1, 2024 00:00:00 UTC)
    private static final long CUSTOM_EPOCH = 1704067200000L;
    
    // Bit allocation
    private static final long NODE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    
    // Maximum values
    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095
    
    // Bit shifts
    private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS;
    
    private final long nodeId;
    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;
    
    // Thread-safe object for synchronization to reduce contention
    private final Object lock = new Object();
    
    public SnowflakeIdGenerator(@Value("${snowflake.node-id:1}") long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                String.format("Node ID must be between 0 and %d, got: %d", MAX_NODE_ID, nodeId));
        }
        this.nodeId = nodeId;
    }
    
    /**
     * Generate a unique 64-bit ID
     * Thread-safe and optimized for high throughput
     * 
     * @return unique 64-bit ID
     * @throws IllegalStateException if clock moves backwards
     */
    public long generateId() {
        synchronized (lock) {
            long currentTimestamp = getCurrentTimestamp();
            
            // Handle clock moving backwards
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= 5) {
                    // Small clock drift - wait it out
                    try {
                        Thread.sleep(offset << 1);
                        currentTimestamp = getCurrentTimestamp();
                        if (currentTimestamp < lastTimestamp) {
                            throw new IllegalStateException(
                                String.format("Clock moved backwards. Last timestamp: %d, current: %d", 
                                            lastTimestamp, currentTimestamp));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while waiting for clock adjustment", e);
                    }
                } else {
                    throw new IllegalStateException(
                        String.format("Clock moved backwards by %d ms. Last timestamp: %d, current: %d", 
                                    offset, lastTimestamp, currentTimestamp));
                }
            }
            
            // Handle same millisecond
            if (currentTimestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0) {
                    // Sequence exhausted for this millisecond - wait for next
                    currentTimestamp = waitForNextMillis(lastTimestamp);
                }
            } else {
                // New millisecond - reset sequence
                // Start with 0 for new millisecond
                sequence = 0L;
            }
            
            lastTimestamp = currentTimestamp;
            
            // Construct the ID
            return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                    | (nodeId << NODE_ID_SHIFT)
                    | sequence;
        }
    }
    
    /**
     * Generate a string representation of the ID for easier handling
     * 
     * @return String representation of unique ID
     */
    public String generateStringId() {
        return String.valueOf(generateId());
    }
    
    /**
     * Batch generate multiple IDs for high-throughput scenarios
     * More efficient than calling generateId() multiple times
     * 
     * @param count number of IDs to generate (max 1000 for safety)
     * @return array of unique IDs
     */
    public long[] generateBatchIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        if (count > 1000) {
            throw new IllegalArgumentException("Batch size too large, maximum 1000 IDs per batch");
        }
        
        long[] ids = new long[count];
        synchronized (lock) {
            for (int i = 0; i < count; i++) {
                ids[i] = generateIdInternal(); // Use internal method to avoid double synchronization
            }
        }
        return ids;
    }
    
    /**
     * Internal ID generation without synchronization (caller must synchronize)
     */
    private long generateIdInternal() {
        long currentTimestamp = getCurrentTimestamp();
        
        // Handle clock moving backwards
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    currentTimestamp = getCurrentTimestamp();
                    if (currentTimestamp < lastTimestamp) {
                        throw new IllegalStateException("Clock moved backwards after waiting");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for clock adjustment", e);
                }
            } else {
                throw new IllegalStateException("Clock moved backwards by " + offset + " ms");
            }
        }
        
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L; // Reset sequence for new millisecond
        }
        
        lastTimestamp = currentTimestamp;
        
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | sequence;
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    private long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * Wait until the next millisecond
     */
    private long waitForNextMillis(long lastTimestamp) {
        long currentTimestamp = getCurrentTimestamp();
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }
    
    /**
     * Get node ID for debugging/monitoring
     */
    public long getNodeId() {
        return nodeId;
    }
    
    /**
     * Get theoretical maximum IDs per second for this node
     */
    public long getMaxIdsPerSecond() {
        return (MAX_SEQUENCE + 1) * 1000L; // 4,096,000 IDs per second
    }
}