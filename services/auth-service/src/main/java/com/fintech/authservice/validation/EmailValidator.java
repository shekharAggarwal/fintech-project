package com.fintech.authservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        // Basic pattern check
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }
        
        // Additional business rules
        String domain = email.substring(email.lastIndexOf("@") + 1);
        
        // Block disposable email providers
        if (isDisposableEmailDomain(domain)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Disposable email addresses are not allowed"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    private boolean isDisposableEmailDomain(String domain) {
        String[] disposableDomains = {
            "10minutemail.com", "tempmail.org", "guerrillamail.com",
            "mailinator.com", "throwaway.email", "temp-mail.org"
        };
        
        String lowerDomain = domain.toLowerCase();
        for (String disposable : disposableDomains) {
            if (lowerDomain.equals(disposable)) {
                return true;
            }
        }
        return false;
    }
}
