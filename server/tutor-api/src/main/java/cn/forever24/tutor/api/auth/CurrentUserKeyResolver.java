package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthApplicationService;
import cn.forever24.tutor.application.auth.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserKeyResolver {

    private static final String LEGACY_DEFAULT_USER_KEY = "local-dev-user";

    private final AuthApplicationService authApplicationService;
    private final boolean legacyUserKeyEnabled;

    @Autowired
    public CurrentUserKeyResolver(
            AuthApplicationService authApplicationService,
            @Value("${tutor.auth.legacy-user-key-enabled:false}") boolean legacyUserKeyEnabled
    ) {
        this.authApplicationService = authApplicationService;
        this.legacyUserKeyEnabled = legacyUserKeyEnabled;
    }

    private CurrentUserKeyResolver(boolean legacyUserKeyEnabled) {
        this.authApplicationService = null;
        this.legacyUserKeyEnabled = legacyUserKeyEnabled;
    }

    public static CurrentUserKeyResolver legacyOnly() {
        return new CurrentUserKeyResolver(true);
    }

    public String resolve(String legacyUserKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)) {
            if (authApplicationService == null) {
                throw AuthException.unauthorized("AUTHENTICATION_REQUIRED", "Authentication is required");
            }
            long userId = parseUserId(authentication.getName());
            return authApplicationService.currentUser(userId).userKey();
        }
        if (legacyUserKeyEnabled) {
            if (legacyUserKey == null || legacyUserKey.isBlank()) {
                return LEGACY_DEFAULT_USER_KEY;
            }
            return legacyUserKey.trim();
        }
        throw AuthException.unauthorized("AUTHENTICATION_REQUIRED", "Authentication is required");
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static long parseUserId(String principalName) {
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException exception) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
    }
}
