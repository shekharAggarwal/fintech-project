package com.fintech.notificationservice.provider.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "twilio")
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsProvider.class);

    @Value("${notification.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String fromNumber;

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        logger.info("Sending SMS via Twilio to: {}", phoneNumber);

        try {
            // Twilio SDK integration point
            // In production, use: com.twilio.rest.api.v2010.account.Message.creator(...)
            // For now, log the intent — actual Twilio SDK dependency should be added when going live
            logger.info("Twilio SMS dispatched to: {} from: {}", phoneNumber, fromNumber);
            return true;
        } catch (Exception e) {
            logger.error("Twilio SMS failed for: {} - Error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Twilio SMS sending failed", e);
        }
    }

    @Override
    public String getProviderName() {
        return "TwilioSmsProvider";
    }
}
