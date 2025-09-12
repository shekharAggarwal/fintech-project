package com.fintech.userservice.repository;

import com.fintech.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(String userId);
    Optional<UserProfile> findByAccountNumber(String accountNumber);
    Optional<UserProfile> findByEmail(String email);
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
    boolean existsByUserId(String userId);
    boolean existsByAccountNumber(String accountNumber);
    
    @Query("SELECT u FROM UserProfile u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "u.phoneNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "u.email LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "u.accountNumber LIKE CONCAT('%', :searchTerm, '%')")
    List<UserProfile> searchUsers(@Param("searchTerm") String searchTerm);
}
