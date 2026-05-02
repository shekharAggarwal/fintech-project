package com.fintech.security.config;

import com.fintech.security.service.AuthorizationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for the fintech security module.
 * Automatically configures all security components when the module is included.
 * <p>
 * Bean creation relies on component scanning via {@code @ComponentScan}.
 * The explicit {@code @Bean} fallback below only activates if component scanning
 * fails to register the service (e.g., in test contexts or unusual classloader setups).
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {
    "com.fintech.security.aspect",
    "com.fintech.security.filter", 
    "com.fintech.security.service",
    "com.fintech.security.util"
})
public class SecurityAutoConfiguration {
    
    /**
     * Fallback bean definition for AuthorizationService.
     * Only created if component scanning does not register one (e.g., in integration tests).
     * AuthorizationService currently has no injected dependencies — if dependencies
     * are added in the future, this method must be updated to wire them explicitly.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService() {
        return new AuthorizationService();
    }
}