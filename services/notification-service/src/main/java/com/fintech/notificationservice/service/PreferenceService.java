package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.DeviceTokenDto;
import com.fintech.notificationservice.dto.PreferenceDto;
import com.fintech.notificationservice.entity.DeviceToken;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.exception.ResourceNotFoundException;
import com.fintech.notificationservice.repository.DeviceTokenRepository;
import com.fintech.notificationservice.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class PreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(PreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public PreferenceService(NotificationPreferenceRepository preferenceRepository,
                             DeviceTokenRepository deviceTokenRepository) {
        this.preferenceRepository = preferenceRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    // ─── Preferences ────────────────────────────────────────────────────────────

    public List<NotificationPreference> getPreferencesForUser(String userId) {
        return preferenceRepository.findByUserId(userId);
    }

    @Transactional
    public NotificationPreference updatePreference(String userId, PreferenceDto dto) {
        logger.info("Updating preference for userId={} channel={}", userId, dto.getChannel());

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndChannel(userId, dto.getChannel())
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference(userId, dto.getChannel(), dto.isEnabled());
                    return newPref;
                });

        pref.setEnabled(dto.isEnabled());
        pref.setQuietHoursStart(dto.getQuietHoursStart());
        pref.setQuietHoursEnd(dto.getQuietHoursEnd());

        return preferenceRepository.save(pref);
    }

    /**
     * Check if the current time falls within the user's quiet hours for this channel.
     */
    public boolean isInQuietHours(NotificationPreference pref) {
        if (pref.getQuietHoursStart() == null || pref.getQuietHoursEnd() == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = pref.getQuietHoursStart();
        LocalTime end = pref.getQuietHoursEnd();

        // Handle overnight quiet hours (e.g., 22:00 → 07:00)
        if (start.isAfter(end)) {
            return now.isAfter(start) || now.isBefore(end);
        }

        return now.isAfter(start) && now.isBefore(end);
    }

    // ─── Device Tokens ──────────────────────────────────────────────────────────

    public List<DeviceToken> getDeviceTokensForUser(String userId) {
        return deviceTokenRepository.findByUserId(userId);
    }

    @Transactional
    public DeviceToken registerDeviceToken(String userId, DeviceTokenDto dto) {
        logger.info("Registering device token for userId={} platform={}", userId, dto.getPlatform());

        // Check if already registered
        return deviceTokenRepository.findByUserIdAndDeviceToken(userId, dto.getDeviceToken())
                .map(existing -> {
                    existing.setEnabled(true);
                    existing.setPlatform(dto.getPlatform());
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> {
                    DeviceToken token = new DeviceToken(userId, dto.getDeviceToken(), dto.getPlatform());
                    return deviceTokenRepository.save(token);
                });
    }

    @Transactional
    public void removeDeviceToken(String userId, String deviceToken) {
        logger.info("Removing device token for userId={}", userId);

        DeviceToken token = deviceTokenRepository.findByUserIdAndDeviceToken(userId, deviceToken)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceToken", "token", deviceToken));

        deviceTokenRepository.delete(token);
    }
}
