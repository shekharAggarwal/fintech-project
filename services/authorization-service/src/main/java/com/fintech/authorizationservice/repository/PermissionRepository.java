package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.ApiMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<ApiMethod, Long> {
//    Optional<ApiMethod> findByKey(String key);
}