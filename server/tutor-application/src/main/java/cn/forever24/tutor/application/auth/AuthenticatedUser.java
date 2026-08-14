package cn.forever24.tutor.application.auth;

import java.util.Set;

public record AuthenticatedUser(
        long userId,
        String userKey,
        String email,
        String status,
        String locale,
        String timezone,
        long authVersion,
        Set<String> roles,
        Set<String> authorities
) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
