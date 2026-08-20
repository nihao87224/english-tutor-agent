package cn.forever24.tutor.entitlement;

import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.util.Objects;

public record Entitlement(
        String entitlementKey,
        UserKey userKey,
        String collectionKey,
        EntitlementStatus status,
        long grantedByUserId,
        Instant grantedAt,
        Instant expiresAt,
        Instant revokedAt,
        String reason,
        long version
) {
    public Entitlement {
        entitlementKey = required(entitlementKey, "entitlementKey");
        Objects.requireNonNull(userKey, "userKey must not be null");
        collectionKey = required(collectionKey, "collectionKey");
        Objects.requireNonNull(status, "status must not be null");
        if (grantedByUserId <= 0) {
            throw new IllegalArgumentException("grantedByUserId must be positive");
        }
        Objects.requireNonNull(grantedAt, "grantedAt must not be null");
        if (expiresAt != null && !expiresAt.isAfter(grantedAt)) {
            throw new IllegalArgumentException("expiresAt must be after grantedAt");
        }
        if (status == EntitlementStatus.REVOKED && revokedAt == null) {
            throw new IllegalArgumentException("revoked entitlement requires revokedAt");
        }
        if (status != EntitlementStatus.REVOKED && revokedAt != null) {
            throw new IllegalArgumentException("only revoked entitlement may have revokedAt");
        }
        if (status == EntitlementStatus.EXPIRED && expiresAt == null) {
            throw new IllegalArgumentException("expired entitlement requires expiresAt");
        }
        reason = normalize(reason);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static Entitlement grant(
            String entitlementKey,
            UserKey userKey,
            String collectionKey,
            long grantedByUserId,
            Instant grantedAt,
            Instant expiresAt,
            String reason
    ) {
        validateFutureExpiry(grantedAt, expiresAt);
        return new Entitlement(
                entitlementKey,
                userKey,
                collectionKey,
                EntitlementStatus.ACTIVE,
                grantedByUserId,
                grantedAt,
                expiresAt,
                null,
                reason,
                0);
    }

    public Entitlement grant(long actorUserId, Instant now, Instant newExpiresAt, String newReason) {
        Objects.requireNonNull(now, "now must not be null");
        validateFutureExpiry(now, newExpiresAt);
        String normalizedReason = normalize(newReason);
        if (status == EntitlementStatus.ACTIVE
                && Objects.equals(expiresAt, newExpiresAt)
                && Objects.equals(reason, normalizedReason)) {
            return this;
        }
        return new Entitlement(
                entitlementKey,
                userKey,
                collectionKey,
                EntitlementStatus.ACTIVE,
                actorUserId,
                now,
                newExpiresAt,
                null,
                normalizedReason,
                version + 1);
    }

    public Entitlement revoke(Instant now, String revokeReason) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == EntitlementStatus.REVOKED) {
            return this;
        }
        return new Entitlement(
                entitlementKey,
                userKey,
                collectionKey,
                EntitlementStatus.REVOKED,
                grantedByUserId,
                grantedAt,
                expiresAt,
                now,
                revokeReason == null ? reason : revokeReason,
                version + 1);
    }

    public Entitlement expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status != EntitlementStatus.ACTIVE || expiresAt == null || expiresAt.isAfter(now)) {
            return this;
        }
        return new Entitlement(
                entitlementKey,
                userKey,
                collectionKey,
                EntitlementStatus.EXPIRED,
                grantedByUserId,
                grantedAt,
                expiresAt,
                null,
                reason,
                version + 1);
    }

    public boolean isActiveAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return status == EntitlementStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(now));
    }

    private static void validateFutureExpiry(Instant now, Instant expiresAt) {
        Objects.requireNonNull(now, "now must not be null");
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("reason must not exceed 500 characters");
        }
        return normalized;
    }
}
