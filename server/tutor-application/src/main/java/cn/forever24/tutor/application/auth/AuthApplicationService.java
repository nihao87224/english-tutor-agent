package cn.forever24.tutor.application.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class AuthApplicationService {

    private static final String DEFAULT_LOCALE = "zh-CN";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final UserAccountRepository userAccountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public AuthApplicationService(
            UserAccountRepository userAccountRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordHasher passwordHasher,
            RefreshTokenService refreshTokenService,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
    }

    public AuthenticatedSession register(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        validatePassword(password);
        if (userAccountRepository.existsByNormalizedEmail(normalizedEmail)) {
            throw AuthException.conflict("EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }
        Instant now = clock.instant();
        String userKey = "usr_" + UUID.randomUUID().toString().replace("-", "");
        AuthenticatedUser user = userAccountRepository.createUser(
                userKey,
                email.trim(),
                normalizedEmail,
                passwordHasher.hash(password),
                DEFAULT_LOCALE,
                DEFAULT_TIMEZONE,
                now);
        userAccountRepository.assignRole(user.userId(), "USER");
        return startSession(userAccountRepository.findById(user.userId()).orElseThrow(), now);
    }

    public AuthenticatedSession login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        UserCredentials credentials = userAccountRepository.findCredentialsByNormalizedEmail(normalizedEmail)
                .orElseThrow(AuthException::invalidCredentials);
        if (!credentials.user().active() || !passwordHasher.matches(password, credentials.passwordHash())) {
            throw AuthException.invalidCredentials();
        }
        Instant now = clock.instant();
        userAccountRepository.updateLastLogin(credentials.user().userId(), now);
        return startSession(userAccountRepository.findById(credentials.user().userId()).orElseThrow(), now);
    }

    public AuthenticatedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.unauthorized("REFRESH_TOKEN_REQUIRED", "Refresh token is required");
        }
        String tokenHash = refreshTokenService.hash(rawRefreshToken);
        StoredRefreshSession storedSession = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> AuthException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        Instant now = clock.instant();
        if (storedSession.revokedAt() != null || !storedSession.expiresAt().isAfter(now)) {
            throw AuthException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }
        AuthenticatedUser user = userAccountRepository.findById(storedSession.userId())
                .orElseThrow(() -> AuthException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        if (!user.active() || user.authVersion() != storedSession.authVersion()) {
            throw AuthException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }

        String newRawToken = refreshTokenService.generate();
        String newSessionId = UUID.randomUUID().toString();
        Instant newExpiresAt = now.plus(REFRESH_TOKEN_TTL);
        refreshSessionRepository.create(new RefreshSessionDraft(
                newSessionId,
                user.userId(),
                refreshTokenService.hash(newRawToken),
                "WEB",
                null,
                user.authVersion(),
                newExpiresAt,
                now));
        refreshSessionRepository.revokeAndReplace(storedSession.id(), newSessionId, now);
        IssuedAccessToken accessToken = accessTokenIssuer.issue(user);
        return new AuthenticatedSession(user, accessToken.token(), accessToken.expiresIn(), newRawToken, newExpiresAt);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshSessionRepository.findByTokenHash(refreshTokenService.hash(rawRefreshToken))
                .ifPresent(session -> refreshSessionRepository.revoke(session.id(), clock.instant()));
    }

    public AuthenticatedUser currentUser(long userId) {
        AuthenticatedUser user = userAccountRepository.findById(userId)
                .orElseThrow(() -> AuthException.unauthorized("USER_NOT_FOUND", "User was not found"));
        if (!user.active()) {
            throw AuthException.unauthorized("USER_DISABLED", "User is not active");
        }
        return user;
    }

    public void bootstrapAdmin(String email, String password) {
        if (userAccountRepository.hasRole("ADMIN")) {
            return;
        }
        String normalizedEmail = EmailNormalizer.normalize(email);
        validatePassword(password);
        Instant now = clock.instant();
        AuthenticatedUser user = userAccountRepository.findByNormalizedEmail(normalizedEmail)
                .orElseGet(() -> userAccountRepository.createUser(
                        "usr_" + UUID.randomUUID().toString().replace("-", ""),
                        email.trim(),
                        normalizedEmail,
                        passwordHasher.hash(password),
                        DEFAULT_LOCALE,
                        DEFAULT_TIMEZONE,
                        now));
        userAccountRepository.assignRole(user.userId(), "USER");
        userAccountRepository.assignRole(user.userId(), "ADMIN");
    }

    private AuthenticatedSession startSession(AuthenticatedUser user, Instant now) {
        IssuedAccessToken accessToken = accessTokenIssuer.issue(user);
        String rawRefreshToken = refreshTokenService.generate();
        Instant refreshExpiresAt = now.plus(REFRESH_TOKEN_TTL);
        refreshSessionRepository.create(new RefreshSessionDraft(
                UUID.randomUUID().toString(),
                user.userId(),
                refreshTokenService.hash(rawRefreshToken),
                "WEB",
                null,
                user.authVersion(),
                refreshExpiresAt,
                now));
        return new AuthenticatedSession(user, accessToken.token(), accessToken.expiresIn(), rawRefreshToken, refreshExpiresAt);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw AuthException.badRequest("WEAK_PASSWORD", "Password must contain at least 8 characters");
        }
    }
}
