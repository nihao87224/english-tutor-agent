package cn.forever24.tutor.application.auth;

import java.time.Instant;

public record RefreshSessionDraft(
        String id,
        long userId,
        String tokenHash,
        String clientType,
        String deviceName,
        long authVersion,
        Instant expiresAt,
        Instant createdAt
) {
}
