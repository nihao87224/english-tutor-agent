package cn.forever24.tutor.infrastructure.auth;

import cn.forever24.tutor.application.auth.AuthenticatedUser;
import cn.forever24.tutor.application.auth.UserAccountRepository;
import cn.forever24.tutor.application.auth.UserCredentials;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, StoredUser> users = new HashMap<>();
    private final Map<String, Long> idByNormalizedEmail = new HashMap<>();
    private final Map<Long, Set<String>> rolesByUser = new HashMap<>();

    @Override
    public synchronized boolean existsByNormalizedEmail(String normalizedEmail) {
        return idByNormalizedEmail.containsKey(normalizedEmail);
    }

    @Override
    public synchronized Optional<UserCredentials> findCredentialsByNormalizedEmail(String normalizedEmail) {
        Long userId = idByNormalizedEmail.get(normalizedEmail);
        if (userId == null) {
            return Optional.empty();
        }
        StoredUser stored = users.get(userId);
        return Optional.of(new UserCredentials(toUser(stored), stored.passwordHash()));
    }

    @Override
    public synchronized Optional<AuthenticatedUser> findByNormalizedEmail(String normalizedEmail) {
        Long userId = idByNormalizedEmail.get(normalizedEmail);
        return userId == null ? Optional.empty() : findById(userId);
    }

    @Override
    public synchronized Optional<AuthenticatedUser> findById(long userId) {
        return Optional.ofNullable(users.get(userId)).map(this::toUser);
    }

    @Override
    public synchronized AuthenticatedUser createUser(
            String userKey,
            String email,
            String normalizedEmail,
            String passwordHash,
            String locale,
            String timezone,
            Instant createdAt
    ) {
        long id = ids.getAndIncrement();
        StoredUser user = new StoredUser(id, userKey, email, normalizedEmail, passwordHash, "ACTIVE", locale, timezone, 0);
        users.put(id, user);
        idByNormalizedEmail.put(normalizedEmail, id);
        return toUser(user);
    }

    @Override
    public synchronized void assignRole(long userId, String roleCode) {
        rolesByUser.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(roleCode);
    }

    @Override
    public synchronized boolean hasRole(String roleCode) {
        return rolesByUser.values().stream().anyMatch(roles -> roles.contains(roleCode));
    }

    @Override
    public void updateLastLogin(long userId, Instant lastLoginAt) {
        // No-op for in-memory tests.
    }

    private AuthenticatedUser toUser(StoredUser user) {
        Set<String> roles = Set.copyOf(rolesByUser.getOrDefault(user.id(), Set.of()));
        Set<String> authorities = roles.contains("ADMIN")
                ? Set.of("DASHBOARD_READ", "USER_READ", "USER_UPDATE", "USER_STATUS_MANAGE",
                "USER_QUOTA_MANAGE", "USER_ROLE_MANAGE", "AI_PROVIDER_READ", "AI_PROVIDER_MANAGE",
                "SYSTEM_SETTING_READ", "SYSTEM_SETTING_MANAGE", "AUDIT_READ")
                : Set.of();
        return new AuthenticatedUser(
                user.id(),
                user.userKey(),
                user.email(),
                user.status(),
                user.locale(),
                user.timezone(),
                user.authVersion(),
                roles,
                authorities);
    }

    private record StoredUser(
            long id,
            String userKey,
            String email,
            String normalizedEmail,
            String passwordHash,
            String status,
            String locale,
            String timezone,
            long authVersion
    ) {
    }
}
