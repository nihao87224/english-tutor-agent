package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthenticatedSession;

public record AuthResponse(UserResponse user, String accessToken, long expiresIn) {

    public static AuthResponse from(AuthenticatedSession session) {
        return new AuthResponse(UserResponse.from(session.user()), session.accessToken(), session.expiresIn());
    }
}
