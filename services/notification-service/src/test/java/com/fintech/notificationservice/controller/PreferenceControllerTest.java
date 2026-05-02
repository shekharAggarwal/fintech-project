package com.fintech.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.notificationservice.dto.DeviceTokenDto;
import com.fintech.notificationservice.dto.PreferenceDto;
import com.fintech.notificationservice.entity.DeviceToken;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.exception.ResourceNotFoundException;
import com.fintech.notificationservice.service.PreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreferenceService preferenceService;

    private ObjectMapper objectMapper;

    private static final String USER_ID = "user-789";
    private static final String BASE_URL = "/api/v1/preferences";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("GET /api/v1/preferences/{userId}")
    class GetPreferences {

        @Test
        @DisplayName("should return 200 with list of preferences")
        void returnsPreferences() throws Exception {
            NotificationPreference emailPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            emailPref.setQuietHoursStart(LocalTime.of(22, 0));
            emailPref.setQuietHoursEnd(LocalTime.of(7, 0));
            NotificationPreference smsPref = new NotificationPreference(USER_ID, NotificationPreference.Channel.SMS, false);

            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(List.of(emailPref, smsPref));

            mockMvc.perform(get(BASE_URL + "/{userId}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].channel", is("EMAIL")))
                    .andExpect(jsonPath("$[0].enabled", is(true)))
                    .andExpect(jsonPath("$[1].channel", is("SMS")))
                    .andExpect(jsonPath("$[1].enabled", is(false)));
        }

        @Test
        @DisplayName("should return 200 with empty list when no preferences exist")
        void returnsEmptyList() throws Exception {
            when(preferenceService.getPreferencesForUser(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/{userId}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/preferences/{userId}")
    class UpdatePreference {

        @Test
        @DisplayName("should return 200 with updated preference")
        void updatesPreference() throws Exception {
            PreferenceDto dto = new PreferenceDto(NotificationPreference.Channel.EMAIL, true,
                    LocalTime.of(22, 0), LocalTime.of(7, 0));

            NotificationPreference updated = new NotificationPreference(USER_ID, NotificationPreference.Channel.EMAIL, true);
            updated.setQuietHoursStart(LocalTime.of(22, 0));
            updated.setQuietHoursEnd(LocalTime.of(7, 0));

            when(preferenceService.updatePreference(eq(USER_ID), any(PreferenceDto.class))).thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.channel", is("EMAIL")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @DisplayName("should return 400 when channel is missing")
        void returnsBadRequestWhenChannelMissing() throws Exception {
            String invalidBody = "{\"enabled\": true}";

            mockMvc.perform(put(BASE_URL + "/{userId}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/preferences/{userId}/devices")
    class GetDeviceTokens {

        @Test
        @DisplayName("should return 200 with list of device tokens")
        void returnsDeviceTokens() throws Exception {
            DeviceToken token = new DeviceToken(USER_ID, "device-token-abc", DeviceToken.Platform.IOS);

            when(preferenceService.getDeviceTokensForUser(USER_ID)).thenReturn(List.of(token));

            mockMvc.perform(get(BASE_URL + "/{userId}/devices", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].deviceToken", is("device-token-abc")))
                    .andExpect(jsonPath("$[0].platform", is("IOS")));
        }

        @Test
        @DisplayName("should return 200 with empty list when no tokens exist")
        void returnsEmptyList() throws Exception {
            when(preferenceService.getDeviceTokensForUser(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/{userId}/devices", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/preferences/{userId}/devices")
    class RegisterDeviceToken {

        @Test
        @DisplayName("should return 201 with registered device token")
        void registersToken() throws Exception {
            DeviceTokenDto dto = new DeviceTokenDto("new-device-token", DeviceToken.Platform.ANDROID);
            DeviceToken savedToken = new DeviceToken(USER_ID, "new-device-token", DeviceToken.Platform.ANDROID);

            when(preferenceService.registerDeviceToken(eq(USER_ID), any(DeviceTokenDto.class))).thenReturn(savedToken);

            mockMvc.perform(post(BASE_URL + "/{userId}/devices", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.deviceToken", is("new-device-token")))
                    .andExpect(jsonPath("$.platform", is("ANDROID")));
        }

        @Test
        @DisplayName("should return 400 when device token is blank")
        void returnsBadRequestWhenTokenBlank() throws Exception {
            String invalidBody = "{\"deviceToken\": \"\", \"platform\": \"IOS\"}";

            mockMvc.perform(post(BASE_URL + "/{userId}/devices", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when platform is missing")
        void returnsBadRequestWhenPlatformMissing() throws Exception {
            String invalidBody = "{\"deviceToken\": \"token-123\"}";

            mockMvc.perform(post(BASE_URL + "/{userId}/devices", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/preferences/{userId}/devices")
    class RemoveDeviceToken {

        @Test
        @DisplayName("should return 204 when device token is removed")
        void removesToken() throws Exception {
            doNothing().when(preferenceService).removeDeviceToken(USER_ID, "token-to-remove");

            mockMvc.perform(delete(BASE_URL + "/{userId}/devices", USER_ID)
                            .param("deviceToken", "token-to-remove"))
                    .andExpect(status().isNoContent());

            verify(preferenceService).removeDeviceToken(USER_ID, "token-to-remove");
        }

        @Test
        @DisplayName("should return 404 when device token not found")
        void returnsNotFoundWhenTokenMissing() throws Exception {
            doThrow(new ResourceNotFoundException("DeviceToken", "token", "nonexistent"))
                    .when(preferenceService).removeDeviceToken(USER_ID, "nonexistent");

            mockMvc.perform(delete(BASE_URL + "/{userId}/devices", USER_ID)
                            .param("deviceToken", "nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }
}
