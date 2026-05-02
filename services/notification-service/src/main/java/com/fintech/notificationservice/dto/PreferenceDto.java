package com.fintech.notificationservice.dto;

import com.fintech.notificationservice.entity.NotificationPreference;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class PreferenceDto {

    @NotNull(message = "Channel is required")
    private NotificationPreference.Channel channel;

    private boolean enabled = true;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;

    public PreferenceDto() {}

    public PreferenceDto(NotificationPreference.Channel channel, boolean enabled,
                         LocalTime quietHoursStart, LocalTime quietHoursEnd) {
        this.channel = channel;
        this.enabled = enabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
    }

    // Conversion helper
    public static PreferenceDto fromEntity(NotificationPreference entity) {
        return new PreferenceDto(
                entity.getChannel(),
                entity.isEnabled(),
                entity.getQuietHoursStart(),
                entity.getQuietHoursEnd()
        );
    }

    public NotificationPreference.Channel getChannel() {
        return channel;
    }

    public void setChannel(NotificationPreference.Channel channel) {
        this.channel = channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
