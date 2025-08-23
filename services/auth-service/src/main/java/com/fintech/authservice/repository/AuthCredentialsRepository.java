package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AuthCredentials entity
 * Handles password and credential operations
 */
@Repository
public interface AuthCredentialsRepository extends JpaRepository<AuthCredentials, Long> {

    @Query("SELECT ac FROM AuthCredentials ac WHERE ac.authCoreId.id = :authCoreId")
    Optional<AuthCredentials> findByAuthCoreId(@Param("authCoreId") Long authCoreId);

}
