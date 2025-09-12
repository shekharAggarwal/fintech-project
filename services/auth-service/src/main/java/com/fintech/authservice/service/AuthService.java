package com.fintech.authservice.service;

import com.fintech.authservice.dto.message.LoginFailureNotification;
import com.fintech.authservice.dto.message.SessionCreationMessage;
import com.fintech.authservice.dto.message.UserCreationMessage;
import com.fintech.authservice.dto.request.RegistrationRequest;
import com.fintech.authservice.dto.response.AuthenticationResult;
import com.fintech.authservice.dto.response.RegistrationResult;
import com.fintech.authservice.entity.AuthCore;
import com.fintech.authservice.entity.AuthCredentials;
import com.fintech.authservice.messaging.EmailNotificationPublisher;
import com.fintech.authservice.messaging.SessionCreationKafkaPublisher;
import com.fintech.authservice.messaging.UserCreationKafkaPublisher;
import com.fintech.authservice.model.AuthCredDB;
import com.fintech.authservice.repository.AuthCoreRepository;
import com.fintech.authservice.repository.AuthCredentialsRepository;
import com.fintech.authservice.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.fintech.authservice.util.SecurityUtils.generateSalt;
import static com.fintech.authservice.util.SecurityUtils.hashPassword;


@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    final private AuthCoreRepository authCoreRepository;

    final private AuthCredentialsRepository credentialsRepository;

    final private UserCreationKafkaPublisher userCreationKafkaPublisher;

    final private SessionService sessionService;

    final private SessionCreationKafkaPublisher sessionCreationKafkaPublisher;

    final private EmailNotificationPublisher emailNotificationPublisher;

    public AuthService(AuthCoreRepository authCoreRepository, AuthCredentialsRepository credentialsRepository,
                       UserCreationKafkaPublisher userCreationKafkaPublisher, SessionService sessionService,
                       SessionCreationKafkaPublisher sessionCreationKafkaPublisher,
                       EmailNotificationPublisher emailNotificationPublisher) {
        this.authCoreRepository = authCoreRepository;
        this.credentialsRepository = credentialsRepository;
        this.userCreationKafkaPublisher = userCreationKafkaPublisher;
        this.sessionService = sessionService;
        this.sessionCreationKafkaPublisher = sessionCreationKafkaPublisher;
        this.emailNotificationPublisher = emailNotificationPublisher;
    }


    public AuthenticationResult authenticate(String email, String password, String ipAddress, String userAgent) {
        try {
            String sanitizedEmail = SecurityUtils.sanitizeInput(email);
            // Step 1: Quick lookup in lean auth_core table
            Optional<AuthCore> authCoreOpt = authCoreRepository.findByEmailAndStatus(sanitizedEmail, AuthCore.AuthStatus.ACTIVE);
            if (authCoreOpt.isEmpty()) {
                // Try other statuses except DELETED
                authCoreOpt = authCoreRepository.findByEmail(sanitizedEmail);
                if (authCoreOpt.isEmpty()) {
                    return AuthenticationResult.failed("Invalid credentials", "USER_NOT_FOUND");
                }

                AuthCore authCore = authCoreOpt.get();
                if (!authCore.canAuthenticate()) {
                    return AuthenticationResult.failed("Account not available", getAccountStatusReason(authCore.getStatus()));
                }
            }

            AuthCore authCore = authCoreOpt.get();
            String userId = authCore.getUserId();

            // Step 3: Verify credentials
            Optional<AuthCredDB> credentialsOpt = credentialsRepository.findByAuthCoreId(authCore.getId());
            if (credentialsOpt.isEmpty()) {
                return AuthenticationResult.failed("Invalid credentials", "CREDENTIALS_NOT_FOUND");
            }

            AuthCredDB credentials = credentialsOpt.get();
            if (!SecurityUtils.verifyPassword(password, credentials.passwordHash(), credentials.salt())) {
                // Handle failed authentication - send email notification
                try {
                    LoginFailureNotification notification = new LoginFailureNotification(
                            sanitizedEmail,
                            ipAddress,
                            userAgent,
                            System.currentTimeMillis()
                    );
                    emailNotificationPublisher.publishLoginFailureEmail(notification);
                    logger.info("Published login failure email notification for email: {}", sanitizedEmail);
                } catch (Exception e) {
                    logger.error("Failed to publish login failure email notification for email: {}", sanitizedEmail, e);
                }

                return AuthenticationResult.failed("Invalid credentials", "INVALID_PASSWORD");
            }

            // Step 4: Create session (2-5ms)
            final String sessionId = SecurityUtils.generateCryptographicallySecureSessionId();

            // Store session in Redis
            try {
                sessionService.storeSession(sessionId, userId);
                logger.info("Session stored in Redis for user: {} with sessionId: {}", userId, sessionId);
            } catch (Exception e) {
                logger.error("Failed to store session in Redis for user: {}", userId, e);
                return AuthenticationResult.failed("Authentication failed", "SESSION_STORAGE_ERROR");
            }

            // Send message to authorization service for session table entry
            try {
                SessionCreationMessage sessionMessage = new SessionCreationMessage(
                        sessionId, userId, LocalDateTime.now());

                sessionCreationKafkaPublisher.publishSessionCreationMessage(sessionMessage);
                logger.info("Published session creation message to Kafka for user: {} with sessionId: {}", userId, sessionId);
            } catch (Exception e) {
                logger.error("Failed to publish session creation message for user: {}", userId, e);
                // Note: We don't fail the authentication if messaging fails
                // The session is already stored in Redis and can be used
            }


            return AuthenticationResult.success(authCore, sessionId);

        } catch (Exception e) {
            // Log error and return generic failure
            return AuthenticationResult.failed("Authentication failed", "SYSTEM_ERROR");
        }
    }

    public RegistrationResult registerUser(RegistrationRequest request) {
        try {
            String sanitizedEmail = SecurityUtils.sanitizeInput(request.email());
            // Check if email already exists
            if (authCoreRepository.existsByEmail(sanitizedEmail)) {
                logger.warn("Registration failed: Email already exists - {}", request.email());
                return RegistrationResult.failed("Email already registered", "EMAIL_EXISTS");
            }

            // Validate initial deposit
            if (request.initialDeposit() != null && request.initialDeposit() < 0) {
                logger.warn("Registration failed: Invalid initial deposit - {}", request.initialDeposit());
                return RegistrationResult.failed("Initial deposit cannot be negative", "INVALID_DEPOSIT");
            }

            String userId = UUID.randomUUID().toString();
            logger.info("Creating new user with ID: {} and email: {}", userId, sanitizedEmail);

            // Create auth core
            AuthCore authCore = new AuthCore(userId, sanitizedEmail);
            authCore = authCoreRepository.save(authCore);

            // Create credentials
            final String passwordSalt = generateSalt();
            String encodedPassword = hashPassword(request.password(), passwordSalt);
            AuthCredentials credentials = new AuthCredentials(authCore.getId(), encodedPassword, passwordSalt);
            credentialsRepository.save(credentials);


            // Publish user creation message to RabbitMQ for user service
            UserCreationMessage userCreationMessage = new UserCreationMessage(
                    userId,
                    request.firstName(),
                    request.lastName(),
                    sanitizedEmail,
                    request.phoneNumber(),
                    request.address(),
                    request.dateOfBirth(),
                    request.occupation(),
                    request.initialDeposit()
            );

            try {
                userCreationKafkaPublisher.publishUserCreationMessage(userCreationMessage);
                logger.info("Published user creation message to Kafka for user: {}", userId);
            } catch (Exception e) {
                logger.error("Failed to publish user creation message to Kafka for user: {}", userId, e);
                // Note: We don't fail the registration if Kafka fails
                // The user profile can be created later through manual process or retry mechanism
            }

            logger.info("User registration completed successfully for: {}", userId);
            return RegistrationResult.success(authCore, "User registered successfully. Profile will be created shortly.");

        } catch (Exception e) {
            logger.error("Registration failed for email: {}", request.email(), e);
            return RegistrationResult.failed("Registration failed", "SYSTEM_ERROR");
        }
    }


    private String getAccountStatusReason(AuthCore.AuthStatus status) {
        return switch (status) {
            case PENDING_VERIFICATION -> "EMAIL_VERIFICATION_REQUIRED";
            case SUSPENDED -> "ACCOUNT_SUSPENDED";
            case LOCKED -> "ACCOUNT_LOCKED";
            case DISABLED -> "ACCOUNT_DISABLED";
            case DELETED -> "ACCOUNT_DELETED";
            default -> "ACCOUNT_INACTIVE";
        };
    }

}
