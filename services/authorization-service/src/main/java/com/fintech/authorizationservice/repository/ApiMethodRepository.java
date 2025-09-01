package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.ApiMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApiMethodRepository extends JpaRepository<ApiMethod, Long> {

    /**
     * Find API method using PostgreSQL path matching function
     * Uses native query to call the is_path_matching database function
     */
    @Query(value = "SELECT am.api_method_id FROM api_methods am WHERE is_path_matching(:path, am.path) = true AND am.http_method = :method", 
           nativeQuery = true)
    Optional<Long> findByPathAndHttpMethod(@Param("path") String path, @Param("method") String method);
    
    /**
     * Find API method by exact path and HTTP method match (fallback)
     */
    @Query("SELECT am.apiMethodId FROM ApiMethod am WHERE am.path = :path AND am.httpMethod = :method")
    Optional<Long> findByExactPathAndHttpMethod(@Param("path") String path, @Param("method") String method);
}
