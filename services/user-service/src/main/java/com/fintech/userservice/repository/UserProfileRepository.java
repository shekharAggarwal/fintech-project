package com.fintech.userservice.repository;

import com.fintech.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(String userId);
    Optional<UserProfile> findByAccountNumber(String accountNumber);
    boolean existsByUserId(String userId);
    boolean existsByAccountNumber(String accountNumber);
}
