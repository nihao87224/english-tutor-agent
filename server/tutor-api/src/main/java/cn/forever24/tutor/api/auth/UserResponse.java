package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthenticatedUser;

import java.util.Set;

public record UserResponse(
        String userKey,
        String email,
        String status,
        Set<String> roles,
        String locale,
        String timezone
) {

    public static UserResponse from(AuthenticatedUser user) {
        return new UserResponse(
                user.userKey(),
                user.email(),
                user.status(),
                user.roles(),
                user.locale(),
                user.timezone());
    }
}
