package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthCredentials;
import com.fintech.authservice.model.AuthCredDB;
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

//    @Query("SELECT AuthCredDB FROM AuthCredentials ac WHERE ac.authCoreId = :authCoreId")
//    Optional<AuthCredDB> findByAuthCoreId(@Param("authCoreId") Long authCoreId);

    Optional<AuthCredDB> findByAuthCoreId(Long authCoreId);

}
