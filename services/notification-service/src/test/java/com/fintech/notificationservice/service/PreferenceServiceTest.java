package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.DeviceTokenDto;
import com.fintech.notificationservice.dto.PreferenceDto;
import com.fintech.notificationservice.entity.DeviceToken;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.exception.ResourceNotFoundException;
import com.fintech.notificationservice.repository.DeviceTokenRepository;
import com.fintech.notificationservice.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    private static final String USER_ID = "user-456";

    @Nested
    @DisplayName("getPreferencesForUser")
    class GetPreferences {

        @Test
        @DisplayName("should return preferences from repository")
        void returnsPreferences() {
            NotificationPreference emailPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            NotificationPreference smsPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.SMS, false);
            when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of(emailPref, smsPref));

            List<NotificationPreference> result = preferenceService.getPreferencesForUser(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getChannel()).isEqualTo(NotificationPreference.Channel.EMAIL);
            assertThat(result.get(1).getChannel()).isEqualTo(NotificationPreference.Channel.SMS);
            verify(preferenceRepository).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("should return empty list when no preferences exist")
        void returnsEmptyList() {
            when(preferenceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<NotificationPreference> result = preferenceService.getPreferencesForUser(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePreference")
    class UpdatePreference {

        @Test
        @DisplayName("should update existing preference")
        void updatesExisting() {
            NotificationPreference existing = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            when(preferenceRepository.findByUserIdAndChannel(USER_ID, NotificationPreference.Channel.EMAIL))
                    .thenReturn(Optional.of(existing));
            when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

            PreferenceDto dto = new PreferenceDto(NotificationPreference.Channel.EMAIL, false,
                    LocalTime.of(22, 0), LocalTime.of(7, 0));

            NotificationPreference result = preferenceService.updatePreference(USER_ID, dto);

            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getQuietHoursStart()).isEqualTo(LocalTime.of(22, 0));
            assertThat(result.getQuietHoursEnd()).isEqualTo(LocalTime.of(7, 0));
            verify(preferenceRepository).save(existing);
        }

        @Test
        @DisplayName("should create new preference when not found")
        void createsNew() {
            when(preferenceRepository.findByUserIdAndChannel(USER_ID, NotificationPreference.Channel.PUSH))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

            PreferenceDto dto = new PreferenceDto(NotificationPreference.Channel.PUSH, true, null, null);

            NotificationPreference result = preferenceService.updatePreference(USER_ID, dto);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getChannel()).isEqualTo(NotificationPreference.Channel.PUSH);
            assertThat(result.isEnabled()).isTrue();

            ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
            verify(preferenceRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("isInQuietHours")
    class IsInQuietHours {

        @Test
        @DisplayName("should return false when quiet hours are not set")
        void returnsFalseWhenNotSet() {
            NotificationPreference pref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);

            boolean result = preferenceService.isInQuietHours(pref);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when only start is set")
        void returnsFalseWhenOnlyStartSet() {
            NotificationPreference pref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            pref.setQuietHoursStart(LocalTime.of(22, 0));

            boolean result = preferenceService.isInQuietHours(pref);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when only end is set")
        void returnsFalseWhenOnlyEndSet() {
            NotificationPreference pref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            pref.setQuietHoursEnd(LocalTime.of(7, 0));

            boolean result = preferenceService.isInQuietHours(pref);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getDeviceTokensForUser")
    class GetDeviceTokens {

        @Test
        @DisplayName("should return device tokens from repository")
        void returnsTokens() {
            DeviceToken token = new DeviceToken(USER_ID, "token-abc", DeviceToken.Platform.IOS);
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(List.of(token));

            List<DeviceToken> result = preferenceService.getDeviceTokensForUser(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeviceToken()).isEqualTo("token-abc");
        }
    }

    @Nested
    @DisplayName("registerDeviceToken")
    class RegisterDeviceToken {

        @Test
        @DisplayName("should register new device token")
        void registersNew() {
            DeviceTokenDto dto = new DeviceTokenDto("new-token", DeviceToken.Platform.ANDROID);
            when(deviceTokenRepository.findByUserIdAndDeviceToken(USER_ID, "new-token"))
                    .thenReturn(Optional.empty());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(i -> i.getArgument(0));

            DeviceToken result = preferenceService.registerDeviceToken(USER_ID, dto);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getDeviceToken()).isEqualTo("new-token");
            assertThat(result.getPlatform()).isEqualTo(DeviceToken.Platform.ANDROID);
        }

        @Test
        @DisplayName("should update existing device token")
        void updatesExisting() {
            DeviceToken existing = new DeviceToken(USER_ID, "existing-token", DeviceToken.Platform.IOS);
            existing.setEnabled(false);
            DeviceTokenDto dto = new DeviceTokenDto("existing-token", DeviceToken.Platform.ANDROID);
            when(deviceTokenRepository.findByUserIdAndDeviceToken(USER_ID, "existing-token"))
                    .thenReturn(Optional.of(existing));
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(i -> i.getArgument(0));

            DeviceToken result = preferenceService.registerDeviceToken(USER_ID, dto);

            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getPlatform()).isEqualTo(DeviceToken.Platform.ANDROID);
        }
    }

    @Nested
    @DisplayName("removeDeviceToken")
    class RemoveDeviceToken {

        @Test
        @DisplayName("should remove existing device token")
        void removesToken() {
            DeviceToken token = new DeviceToken(USER_ID, "token-xyz", DeviceToken.Platform.WEB);
            when(deviceTokenRepository.findByUserIdAndDeviceToken(USER_ID, "token-xyz"))
                    .thenReturn(Optional.of(token));

            preferenceService.removeDeviceToken(USER_ID, "token-xyz");

            verify(deviceTokenRepository).delete(token);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when token not found")
        void throwsWhenNotFound() {
            when(deviceTokenRepository.findByUserIdAndDeviceToken(USER_ID, "nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> preferenceService.removeDeviceToken(USER_ID, "nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
