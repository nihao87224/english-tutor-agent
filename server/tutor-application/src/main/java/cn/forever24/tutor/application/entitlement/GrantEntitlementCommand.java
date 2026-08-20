package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.util.Objects;

public record GrantEntitlementCommand(
        UserKey userKey,
        String collectionKey,
        Instant expiresAt,
        String reason
) {
    public GrantEntitlementCommand {
        Objects.requireNonNull(userKey, "userKey must not be null");
        collectionKey = required(collectionKey);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("collectionKey is required");
        }
        return value.trim();
    }
}
