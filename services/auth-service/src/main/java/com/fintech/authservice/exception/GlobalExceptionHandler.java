package com.fintech.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .path(request.getRequestURI())
                .traceId(generateTraceId())
                .validationErrors(errors)
                .build();
                
        logger.warn("Validation error: {} for request: {}", errors, request.getRequestURI());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid credentials")
                .path(request.getRequestURI())
                .traceId(generateTraceId())
                .build();
                
        logger.warn("Authentication failed: {} for request: {}", ex.getMessage(), request.getRequestURI());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("Insufficient permissions")
                .path(request.getRequestURI())
                .traceId(generateTraceId())
                .build();
                
        logger.warn("Access denied: {} for request: {}", ex.getMessage(), request.getRequestURI());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Rate Limit Exceeded")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(generateTraceId())
                .build();
                
        logger.warn("Rate limit exceeded: {} for request: {}", ex.getMessage(), request.getRequestURI());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }
    
    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceException(
            AuthServiceException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(generateTraceId())
                .build();
                
        logger.error("Auth service error: {} for request: {}", ex.getMessage(), request.getRequestURI(), ex);
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();
                
        logger.error("Unexpected error [traceId: {}]: {} for request: {}", traceId, ex.getMessage(), request.getRequestURI(), ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String traceId;
        private Map<String, String> validationErrors;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ErrorResponse response = new ErrorResponse();
            
            public Builder timestamp(LocalDateTime timestamp) {
                response.timestamp = timestamp;
                return this;
            }
            
            public Builder status(int status) {
                response.status = status;
                return this;
            }
            
            public Builder error(String error) {
                response.error = error;
                return this;
            }
            
            public Builder message(String message) {
                response.message = message;
                return this;
            }
            
            public Builder path(String path) {
                response.path = path;
                return this;
            }
            
            public Builder traceId(String traceId) {
                response.traceId = traceId;
                return this;
            }
            
            public Builder validationErrors(Map<String, String> validationErrors) {
                response.validationErrors = validationErrors;
                return this;
            }
            
            public ErrorResponse build() {
                return response;
            }
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public String getTraceId() { return traceId; }
        public Map<String, String> getValidationErrors() { return validationErrors; }
    }
}
