package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.UserSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends CrudRepository<UserSession, String> {
    
    Optional<UserSession> findByUserEmail(String userEmail);
    
    void deleteByUserEmail(String userEmail);
}
