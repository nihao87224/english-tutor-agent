package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthApplicationService;
import cn.forever24.tutor.application.auth.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.LongFunction;

@Component
public class CurrentUserKeyResolver {

    private final LongFunction<String> userKeyLookup;

    @Autowired
    public CurrentUserKeyResolver(AuthApplicationService authApplicationService) {
        this(userId -> authApplicationService.currentUser(userId).userKey());
    }

    public CurrentUserKeyResolver(LongFunction<String> userKeyLookup) {
        this.userKeyLookup = Objects.requireNonNull(userKeyLookup, "userKeyLookup must not be null");
    }

    public String resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)) {
            long userId = parseUserId(authentication.getName());
            return userKeyLookup.apply(userId);
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
