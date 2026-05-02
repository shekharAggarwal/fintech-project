package com.fintech.notificationservice.provider.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        logger.info("[MOCK SMS] Sending to: {} | Message: {}", phoneNumber, message);
        // Simulate successful send
        return true;
    }

    @Override
    public String getProviderName() {
        return "MockSmsProvider";
    }
}
