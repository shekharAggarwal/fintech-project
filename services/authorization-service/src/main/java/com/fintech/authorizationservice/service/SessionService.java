package com.fintech.authorizationservice.service;

import com.fintech.authorizationservice.entity.Session;
import com.fintech.authorizationservice.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Create a new session in the database
     */
    public Session createSession(String sessionId, String userId, Long expiryTime) {
        try {
            Session session = new Session(sessionId, userId, expiryTime);
            Session savedSession = sessionRepository.save(session);
            
            logger.info("Session created successfully: sessionId={}, userId={}", sessionId, userId);
            return savedSession;
            
        } catch (Exception e) {
            logger.error("Failed to create session: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Get session by sessionId
     */
    public Optional<Session> getSession(String sessionId) {
        try {
            Optional<Session> session = sessionRepository.findBySessionId(sessionId);
            
            if (session.isPresent() && isSessionExpired(session.get())) {
                invalidateSession(sessionId);
                return Optional.empty();
            }
            
            return session;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve session: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Get all active sessions for a user
     */
    public List<Session> getActiveSessionsForUser(String userId) {
        try {
            long currentTime = System.currentTimeMillis();
            return sessionRepository.findActiveSessionsByUserId(userId, currentTime);
        } catch (Exception e) {
            logger.error("Failed to get active sessions for user: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * Invalidate a specific session
     */
    public void invalidateSession(String sessionId) {
        try {
            sessionRepository.deleteBySessionId(sessionId);
            logger.info("Session invalidated: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.error("Failed to invalidate session: sessionId={}", sessionId, e);
        }
    }

    /**
     * Invalidate all sessions for a user
     */
    public void invalidateAllUserSessions(String userId) {
        try {
            sessionRepository.deleteByUserId(userId);
            logger.info("All sessions invalidated for user: userId={}", userId);
        } catch (Exception e) {
            logger.error("Failed to invalidate all sessions for user: userId={}", userId, e);
        }
    }

    /**
     * Clean up expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            long currentTime = System.currentTimeMillis();
            sessionRepository.deleteExpiredSessions(currentTime);
            logger.info("Expired sessions cleaned up successfully");
        } catch (Exception e) {
            logger.error("Failed to cleanup expired sessions", e);
        }
    }

    /**
     * Check if session is expired
     */
    private boolean isSessionExpired(Session session) {
        return System.currentTimeMillis() > session.getExpiryTime();
    }

    /**
     * Validate if session is active
     */
    public boolean isSessionValid(String sessionId) {
        Optional<Session> session = getSession(sessionId);
        return session.isPresent();
    }
}
