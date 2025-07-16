package com.fintech.authservice.repository;

import com.fintech.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByAccountNumber(String accountNumber);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.accountNumber = :accountNumber AND u.isActive = true")
    Optional<User> findActiveUserByAccountNumber(@Param("accountNumber") String accountNumber);
    
    Optional<User> findByRefreshToken(String refreshToken);
    
    @Query("SELECT u FROM User u WHERE u.currentSessionId = :sessionId AND u.isActive = true")
    Optional<User> findByCurrentSessionId(@Param("sessionId") String sessionId);
    
    boolean existsByEmail(String email);
    
    boolean existsByAccountNumber(String accountNumber);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.shardKey = :shardKey")
    long countByShardKey(@Param("shardKey") Integer shardKey);
}
