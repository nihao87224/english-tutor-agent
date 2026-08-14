package cn.forever24.tutor.application.auth;

import java.time.Instant;

public record AuthenticatedSession(
        AuthenticatedUser user,
        String accessToken,
        long expiresIn,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
