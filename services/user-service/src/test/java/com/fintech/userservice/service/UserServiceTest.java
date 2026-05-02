package com.fintech.userservice.service;

import com.fintech.userservice.dto.message.UserCreationMessage;
import com.fintech.userservice.dto.request.UpdateUserRequest;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.entity.enums.KycLevel;
import com.fintech.userservice.entity.enums.KycStatus;
import com.fintech.userservice.external.model.response.UpdateRoleResponse;
import com.fintech.userservice.external.service.AuthzService;
import com.fintech.userservice.messaging.AccountCreationKafkaPublisher;
import com.fintech.userservice.messaging.AuthorizationKafkaPublisher;
import com.fintech.userservice.messaging.EmailNotificationPublisher;
import com.fintech.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private EmailNotificationPublisher emailNotificationPublisher;

    @Mock
    private AuthorizationKafkaPublisher authorizationKafkaPublisher;

    @Mock
    private AccountCreationKafkaPublisher accountCreationKafkaPublisher;

    @Mock
    private AuthzService authzService;

    @InjectMocks
    private UserService userService;

    private UserProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = new UserProfile(
                "user-123", "John", "Doe", "john@example.com",
                "+1234567890", "123 Main St", "1990-01-01",
                "Engineer", 1000.0, "ACCOUNT_HOLDER", "000000000001"
        );
    }

    @Nested
    @DisplayName("createUserProfile")
    class CreateUserProfile {

        @Test
        @DisplayName("should create user profile successfully")
        void shouldCreateUserProfileSuccessfully() {
            UserCreationMessage message = new UserCreationMessage(
                    "user-123", "John", "Doe", "john@example.com",
                    "+1234567890", "123 Main St", "1990-01-01",
                    "Engineer", 1000.0
            );

            when(userProfileRepository.existsByUserId("user-123")).thenReturn(false);
            when(userProfileRepository.existsByAccountNumber(anyString())).thenReturn(false);
            when(userProfileRepository.save(any(UserProfile.class))).thenReturn(sampleProfile);

            userService.createUserProfile(message);

            verify(userProfileRepository).save(any(UserProfile.class));
            verify(accountCreationKafkaPublisher).publishUserRoleRegistration(any());
            verify(authorizationKafkaPublisher).publishUserRoleRegistration(any());
            verify(emailNotificationPublisher).publishUserGreetingEmail(any());
        }

        @Test
        @DisplayName("should skip creation if user already exists")
        void shouldSkipIfUserAlreadyExists() {
            UserCreationMessage message = new UserCreationMessage(
                    "user-123", "John", "Doe", "john@example.com",
                    "+1234567890", "123 Main St", "1990-01-01",
                    "Engineer", 1000.0
            );

            when(userProfileRepository.existsByUserId("user-123")).thenReturn(true);

            userService.createUserProfile(message);

            verify(userProfileRepository, never()).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should still create profile even if kafka publishing fails")
        void shouldCreateProfileEvenIfKafkaFails() {
            UserCreationMessage message = new UserCreationMessage(
                    "user-123", "John", "Doe", "john@example.com",
                    "+1234567890", "123 Main St", "1990-01-01",
                    "Engineer", 1000.0
            );

            when(userProfileRepository.existsByUserId("user-123")).thenReturn(false);
            when(userProfileRepository.existsByAccountNumber(anyString())).thenReturn(false);
            when(userProfileRepository.save(any(UserProfile.class))).thenReturn(sampleProfile);
            doThrow(new RuntimeException("Kafka down")).when(accountCreationKafkaPublisher).publishUserRoleRegistration(any());

            userService.createUserProfile(message);

            verify(userProfileRepository).save(any(UserProfile.class));
        }
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("should return user profile when found")
        void shouldReturnUserProfileWhenFound() {
            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));

            Optional<UserProfile> result = userService.getUserProfile("user-123");

            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo("user-123");
            assertThat(result.get().getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

            Optional<UserProfile> result = userService.getUserProfile("non-existent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUserProfileFromRequest")
    class UpdateUserProfileFromRequest {

        @Test
        @DisplayName("should update all provided fields")
        void shouldUpdateAllProvidedFields() {
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Smith", "+9876543210", "456 Oak Ave");

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

            UserProfile result = userService.updateUserProfileFromRequest("user-123", request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getPhoneNumber()).isEqualTo("+9876543210");
            assertThat(result.getAddress()).isEqualTo("456 Oak Ave");
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Jane");

            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

            UserProfile result = userService.updateUserProfileFromRequest("user-123", request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getPhoneNumber()).isEqualTo("+1234567890");
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Smith", null, null);
            when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfileFromRequest("non-existent", request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User profile not found");
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("should return matching users")
        void shouldReturnMatchingUsers() {
            List<UserProfile> profiles = List.of(sampleProfile);
            when(userProfileRepository.searchUsers("John")).thenReturn(profiles);

            List<UserProfile> results = userService.searchUsers("John");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should return empty list for null search term")
        void shouldReturnEmptyForNullSearchTerm() {
            List<UserProfile> results = userService.searchUsers(null);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank search term")
        void shouldReturnEmptyForBlankSearchTerm() {
            List<UserProfile> results = userService.searchUsers("   ");
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should trim search term before querying")
        void shouldTrimSearchTerm() {
            when(userProfileRepository.searchUsers("John")).thenReturn(List.of(sampleProfile));

            userService.searchUsers("  John  ");

            verify(userProfileRepository).searchUsers("John");
        }
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRole {

        @Test
        @DisplayName("should change role successfully")
        void shouldChangeRoleSuccessfully() {
            UpdateRoleResponse authzResponse = new UpdateRoleResponse();
            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(authzService.updateRole("user-123", "ADMIN", "admin-1")).thenReturn(Mono.just(authzResponse));
            when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

            UserProfile result = userService.changeUserRole("user-123", "ADMIN", "admin-1");

            assertThat(result.getRole()).isEqualTo("ADMIN");
            verify(authzService).updateRole("user-123", "ADMIN", "admin-1");
            verify(userProfileRepository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userProfileRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeUserRole("non-existent", "ADMIN", "admin-1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User profile not found");
        }

        @Test
        @DisplayName("should throw when authz service returns null")
        void shouldThrowWhenAuthzReturnsNull() {
            when(userProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
            when(authzService.updateRole("user-123", "ADMIN", "admin-1")).thenReturn(Mono.empty());

            assertThatThrownBy(() -> userService.changeUserRole("user-123", "ADMIN", "admin-1"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
