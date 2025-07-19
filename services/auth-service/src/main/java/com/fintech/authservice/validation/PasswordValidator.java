package com.fintech.authservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // Check pattern
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least 8 characters, including uppercase, lowercase, digit and special character"
            ).addConstraintViolation();
            return false;
        }
        
        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        if (isCommonPassword(lowerPassword)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Password is too common. Please choose a stronger password"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty", 
            "letmein", "welcome", "monkey", "dragon", "password1"
        };
        
        for (String common : commonPasswords) {
            if (password.contains(common)) {
                return true;
            }
        }
        return false;
    }
}
