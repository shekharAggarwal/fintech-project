package com.fintech.notificationservice.provider.sms;

/**
 * Interface for SMS provider implementations.
 */
public interface SmsProvider {

    /**
     * Send an SMS message.
     *
     * @param phoneNumber the recipient phone number
     * @param message     the SMS body
     * @return true if sent successfully
     */
    boolean sendSms(String phoneNumber, String message);

    /**
     * Returns the provider name for logging/identification.
     */
    String getProviderName();
}
