package com.fintech.notificationservice.provider.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "mock", matchIfMissing = true)
public class MockPushProvider implements PushProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockPushProvider.class);

    @Override
    public boolean sendPush(String deviceToken, String title, String body, Map<String, String> data) {
        logger.info("[MOCK PUSH] Token: {} | Title: {} | Body: {}", deviceToken, title, body);
        return true;
    }

    @Override
    public String getProviderName() {
        return "MockPushProvider";
    }
}
