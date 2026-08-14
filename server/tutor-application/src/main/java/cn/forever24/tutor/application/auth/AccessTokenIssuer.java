package cn.forever24.tutor.application.auth;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(AuthenticatedUser user);
}
