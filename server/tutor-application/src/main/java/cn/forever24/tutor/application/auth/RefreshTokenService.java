package cn.forever24.tutor.application.auth;

public interface RefreshTokenService {

    String generate();

    String hash(String rawToken);
}
