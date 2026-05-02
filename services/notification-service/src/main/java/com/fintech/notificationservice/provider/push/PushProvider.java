package com.fintech.notificationservice.provider.push;

/**
 * Interface for push notification provider implementations.
 */
public interface PushProvider {

    /**
     * Send a push notification to a specific device token.
     *
     * @param deviceToken the target device token
     * @param title       notification title
     * @param body        notification body
     * @param data        optional key-value data payload (can be null)
     * @return true if sent successfully
     */
    boolean sendPush(String deviceToken, String title, String body, java.util.Map<String, String> data);

    /**
     * Returns the provider name for logging/identification.
     */
    String getProviderName();
}
