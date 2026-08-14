package cn.forever24.tutor.application.admin;

import java.time.Instant;
import java.util.Set;

public record AdminUserDetail(
        long userId,
        String userKey,
        String email,
        String status,
        String locale,
        String timezone,
        long authVersion,
        Set<String> roles,
        Set<String> authorities,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        Instant disabledAt
) {
}
