package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findBySessionId(String sessionId);
    
    List<Session> findByUserId(String userId);
    
    @Query("SELECT s FROM Session s WHERE s.expiryTime > :currentTime")
    List<Session> findActiveSessions(@Param("currentTime") Long currentTime);
    
    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.expiryTime > :currentTime")
    List<Session> findActiveSessionsByUserId(@Param("userId") String userId, @Param("currentTime") Long currentTime);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiryTime <= :currentTime")
    void deleteExpiredSessions(@Param("currentTime") Long currentTime);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
