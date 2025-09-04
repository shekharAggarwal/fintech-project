package com.fintech.userservice.security.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.userservice.security.model.AuthorizationContext;
import com.fintech.userservice.security.util.AuthorizationContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Authorization filter that extracts authorization information from gateway headers
 * and makes it available throughout the request processing chain.
 * 
 * This filter follows the patterns from Spring Security authorization examples:
 * - Extracts authorization context from trusted headers
 * - Provides thread-local access to authorization information
 * - Enables field-level filtering and resource-based authorization
 */
@Component
@Order(1)
public class AuthorizationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final String AUTHZ_HEADER = "X-Authz";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-Role";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Extract authorization information from headers
            AuthorizationContext context = extractAuthorizationContext(httpRequest);
            
            if (context != null) {
                // Set context in thread-local storage
                AuthorizationContextHolder.setContext(context);
                
                logger.debug("Authorization context set for user: {} with role: {} for path: {}", 
                    context.getUserId(), context.getRole(), httpRequest.getRequestURI());
                
                // Check if the request is allowed
                if (!context.isAllowed()) {
                    logger.warn("Access denied for user: {} to path: {} - Reason: {}", 
                        context.getUserId(), httpRequest.getRequestURI(), context.getReason());
                    
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.getWriter().write("{\"error\":\"Access denied\",\"reason\":\"" + 
                        context.getReason() + "\"}");
                    httpResponse.setContentType("application/json");
                    return;
                }
            } else {
                logger.debug("No authorization context found for path: {}", httpRequest.getRequestURI());
            }
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Always clear the context after request processing
            AuthorizationContextHolder.clearContext();
        }
    }

    /**
     * Extract authorization context from gateway headers
     */
    private AuthorizationContext extractAuthorizationContext(HttpServletRequest request) {
        String authzHeader = request.getHeader(AUTHZ_HEADER);
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String roleHeader = request.getHeader(ROLE_HEADER);
        
        // If no authorization headers present, this might be a public endpoint
        if (authzHeader == null && userIdHeader == null && roleHeader == null) {
            return null;
        }
        
        try {
            AuthorizationContext context = new AuthorizationContext();
            
            // Parse the base64 encoded authorization data from gateway
            if (authzHeader != null && !authzHeader.isEmpty()) {
                String decoded = new String(Base64.decodeBase64(authzHeader), StandardCharsets.UTF_8);
                
                // Parse the JSON response from authorization service
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                Map<String, Object> authzData = objectMapper.readValue(decoded, typeRef);
                
                context.setAllowed((Boolean) authzData.getOrDefault("allowed", false));
                context.setUserId((String) authzData.get("userId"));
                context.setRole((String) authzData.get("role"));
                context.setReason((String) authzData.get("reason"));
                
                // Parse permissions
                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) authzData.get("permissions");
                context.setPermissions(permissions);
                
                // Parse resourceAccess (new format)
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> resourceAccess = (Map<String, Map<String, Object>>) authzData.get("resourceAccess");
                context.setResourceAccess(resourceAccess);
                
            } else {
                // Fallback to individual headers if X-Authz is not present
                context.setAllowed(true); // Assume allowed if individual headers are present
                context.setUserId(userIdHeader);
                context.setRole(roleHeader);
            }
            
            return context;
            
        } catch (Exception e) {
            logger.error("Failed to parse authorization headers", e);
            
            // Create a denied context on parsing error
            AuthorizationContext deniedContext = new AuthorizationContext();
            deniedContext.setAllowed(false);
            deniedContext.setReason("INVALID_AUTHZ_HEADERS");
            return deniedContext;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthorizationFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("AuthorizationFilter destroyed");
    }
}
