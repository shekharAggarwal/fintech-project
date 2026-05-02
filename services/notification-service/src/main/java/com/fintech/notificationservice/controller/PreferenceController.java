package com.fintech.notificationservice.controller;

import com.fintech.notificationservice.dto.DeviceTokenDto;
import com.fintech.notificationservice.dto.PreferenceDto;
import com.fintech.notificationservice.entity.DeviceToken;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.service.PreferenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(PreferenceController.class);

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    // ─── Notification Preferences ───────────────────────────────────────────────

    @GetMapping("/{userId}")
    public ResponseEntity<List<PreferenceDto>> getPreferences(@PathVariable String userId) {
        logger.info("GET preferences for userId={}", userId);

        List<NotificationPreference> prefs = preferenceService.getPreferencesForUser(userId);
        List<PreferenceDto> dtos = prefs.stream()
                .map(PreferenceDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<PreferenceDto> updatePreference(
            @PathVariable String userId,
            @Valid @RequestBody PreferenceDto preferenceDto) {
        logger.info("PUT preference for userId={} channel={}", userId, preferenceDto.getChannel());

        NotificationPreference updated = preferenceService.updatePreference(userId, preferenceDto);
        return ResponseEntity.ok(PreferenceDto.fromEntity(updated));
    }

    // ─── Device Tokens ──────────────────────────────────────────────────────────

    @GetMapping("/{userId}/devices")
    public ResponseEntity<List<DeviceToken>> getDeviceTokens(@PathVariable String userId) {
        logger.info("GET device tokens for userId={}", userId);
        return ResponseEntity.ok(preferenceService.getDeviceTokensForUser(userId));
    }

    @PostMapping("/{userId}/devices")
    public ResponseEntity<DeviceToken> registerDeviceToken(
            @PathVariable String userId,
            @Valid @RequestBody DeviceTokenDto deviceTokenDto) {
        logger.info("POST device token for userId={} platform={}", userId, deviceTokenDto.getPlatform());

        DeviceToken token = preferenceService.registerDeviceToken(userId, deviceTokenDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @DeleteMapping("/{userId}/devices")
    public ResponseEntity<Void> removeDeviceToken(
            @PathVariable String userId,
            @RequestParam String deviceToken) {
        logger.info("DELETE device token for userId={}", userId);

        preferenceService.removeDeviceToken(userId, deviceToken);
        return ResponseEntity.noContent().build();
    }
}
