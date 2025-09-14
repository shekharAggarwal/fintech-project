package com.fintech.security.config;

import com.fintech.security.service.AuthorizationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for the fintech security module
 * Automatically configures all security components when the module is included
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
     * Explicit bean definition for AuthorizationService to ensure it's available
     * This helps when component scanning might not work properly
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService() {
        return new AuthorizationService();
    }
}