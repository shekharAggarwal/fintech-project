package com.fintech.authservice.repository;

import com.fintech.authservice.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.userId = :userId ORDER BY e.timestamp DESC")
    List<SecurityEvent> findByUserIdOrderByTimestampDesc(@Param("userId") String userId);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.userId = :userId AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findByUserIdAndTimestampAfter(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.eventType = :eventType AND e.timestamp >= :since")
    List<SecurityEvent> findByEventTypeAndTimestampAfter(@Param("eventType") SecurityEvent.EventType eventType, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.riskLevel = :riskLevel AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findByRiskLevelAndTimestampAfter(@Param("riskLevel") SecurityEvent.RiskLevel riskLevel, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.successful = false AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findFailedEventsAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.userId = :userId AND e.eventType = :eventType AND e.successful = false AND e.timestamp >= :since")
    List<SecurityEvent> findFailedEventsByUserAndType(@Param("userId") String userId, @Param("eventType") SecurityEvent.EventType eventType, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.ipAddress = :ipAddress AND e.successful = false AND e.timestamp >= :since")
    List<SecurityEvent> findFailedEventsByIpAndTimestampAfter(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.userId = :userId AND e.eventType = :eventType AND e.timestamp >= :since")
    long countByUserIdAndEventTypeAndTimestampAfter(@Param("userId") String userId, @Param("eventType") SecurityEvent.EventType eventType, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.ipAddress = :ipAddress AND e.successful = false AND e.timestamp >= :since")
    long countFailedEventsByIpAndTimestampAfter(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.correlationId = :correlationId ORDER BY e.timestamp")
    List<SecurityEvent> findByCorrelationId(@Param("correlationId") String correlationId);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.sessionId = :sessionId ORDER BY e.timestamp DESC")
    List<SecurityEvent> findBySessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT DISTINCT e.ipAddress FROM SecurityEvent e WHERE e.userId = :userId AND e.timestamp >= :since")
    List<String> findDistinctIpAddressesByUserIdAndTimestampAfter(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.riskLevel IN ('HIGH', 'CRITICAL') AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findHighRiskEventsAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM SecurityEvent e WHERE e.eventType IN :eventTypes AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findByEventTypesAndTimestampAfter(@Param("eventTypes") List<SecurityEvent.EventType> eventTypes, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.riskLevel = :riskLevel AND e.timestamp >= :since")
    long countByRiskLevelAndTimestampAfter(@Param("riskLevel") SecurityEvent.RiskLevel riskLevel, @Param("since") LocalDateTime since);
}
