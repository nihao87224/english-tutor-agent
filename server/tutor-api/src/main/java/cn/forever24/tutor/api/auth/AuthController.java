package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthApplicationService;
import cn.forever24.tutor.application.auth.AuthenticatedSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "ETA_REFRESH_TOKEN";

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request, HttpServletResponse response) {
        AuthenticatedSession session = authApplicationService.register(request.email(), request.password());
        addRefreshCookie(response, session);
        return AuthResponse.from(session);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request, HttpServletResponse response) {
        AuthenticatedSession session = authApplicationService.login(request.email(), request.password());
        addRefreshCookie(response, session);
        return AuthResponse.from(session);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = request != null && request.refreshToken() != null
                ? request.refreshToken()
                : cookieRefreshToken;
        AuthenticatedSession session = authApplicationService.refresh(refreshToken);
        addRefreshCookie(response, session);
        return AuthResponse.from(session);
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = request != null && request.refreshToken() != null
                ? request.refreshToken()
                : cookieRefreshToken;
        authApplicationService.logout(refreshToken);
        expireRefreshCookie(response);
    }

    private static void addRefreshCookie(HttpServletResponse response, AuthenticatedSession session) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.between(java.time.Instant.now(), session.refreshTokenExpiresAt()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
