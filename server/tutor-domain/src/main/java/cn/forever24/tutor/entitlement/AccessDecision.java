package cn.forever24.tutor.entitlement;

import java.time.Instant;
import java.util.Objects;

public record AccessDecision(
        boolean allowed,
        AccessDecisionReason reason,
        String collectionKey,
        Instant evaluatedAt
) {
    public AccessDecision {
        Objects.requireNonNull(reason, "reason must not be null");
        if (collectionKey == null || collectionKey.isBlank()) {
            throw new IllegalArgumentException("collectionKey is required");
        }
        collectionKey = collectionKey.trim();
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if (allowed != reason.name().startsWith("ALLOWED_")) {
            throw new IllegalArgumentException("allowed flag and reason disagree");
        }
    }

    public static AccessDecision allow(AccessDecisionReason reason, String collectionKey, Instant now) {
        return new AccessDecision(true, reason, collectionKey, now);
    }

    public static AccessDecision deny(AccessDecisionReason reason, String collectionKey, Instant now) {
        return new AccessDecision(false, reason, collectionKey, now);
    }
}
