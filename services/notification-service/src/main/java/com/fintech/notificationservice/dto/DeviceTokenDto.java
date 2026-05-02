package com.fintech.notificationservice.dto;

import com.fintech.notificationservice.entity.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeviceTokenDto {

    @NotBlank(message = "Device token is required")
    private String deviceToken;

    @NotNull(message = "Platform is required")
    private DeviceToken.Platform platform;

    public DeviceTokenDto() {}

    public DeviceTokenDto(String deviceToken, DeviceToken.Platform platform) {
        this.deviceToken = deviceToken;
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public DeviceToken.Platform getPlatform() {
        return platform;
    }

    public void setPlatform(DeviceToken.Platform platform) {
        this.platform = platform;
    }
}
