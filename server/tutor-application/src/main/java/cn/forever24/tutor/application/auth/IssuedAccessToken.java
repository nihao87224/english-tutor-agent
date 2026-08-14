package cn.forever24.tutor.application.auth;

public record IssuedAccessToken(String token, long expiresIn) {
}
