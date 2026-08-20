package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.profile.UserKey;

import java.util.Objects;

public record RevokeEntitlementCommand(UserKey userKey, String collectionKey, String reason) {
    public RevokeEntitlementCommand {
        Objects.requireNonNull(userKey, "userKey must not be null");
        if (collectionKey == null || collectionKey.isBlank()) {
            throw new IllegalArgumentException("collectionKey is required");
        }
        collectionKey = collectionKey.trim();
    }
}
