package cn.forever24.tutor.application.auth;

public record UserCredentials(AuthenticatedUser user, String passwordHash) {
}
