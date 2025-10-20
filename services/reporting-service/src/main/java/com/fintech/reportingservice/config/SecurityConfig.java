package com.fintech.reportingservice.config;

import com.fintech.security.filter.AuthorizationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Security configuration for reporting service
 */

@Configuration
@EnableAspectJAutoProxy
public class SecurityConfig {

    private final AuthorizationFilter authorizationFilter;

    public SecurityConfig(AuthorizationFilter authorizationFilter) {
        this.authorizationFilter = authorizationFilter;
    }

    /**
     * Register the authorization filter
     */
    @Bean
    public FilterRegistrationBean<AuthorizationFilter> authorizationFilterRegistration() {
        FilterRegistrationBean<AuthorizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authorizationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        registration.setName("authorizationFilter");
        return registration;
    }
}