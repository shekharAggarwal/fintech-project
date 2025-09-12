package com.fintech.authservice.dto.message;

import java.time.LocalDateTime;

public record SessionCreationMessage(
        String sessionId,
        String userId,
        LocalDateTime createdAt
) {
}
