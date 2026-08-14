package cn.forever24.tutor.application.admin;

import java.time.Instant;
import java.util.Set;

public record AdminUserSummary(
        long userId,
        String userKey,
        String email,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant lastLoginAt
) {
}
