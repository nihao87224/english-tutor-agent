package cn.forever24.tutor.application.auth;

import java.time.Instant;
import java.util.Optional;

public interface UserAccountRepository {

    boolean existsByNormalizedEmail(String normalizedEmail);

    Optional<UserCredentials> findCredentialsByNormalizedEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findByNormalizedEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findById(long userId);

    AuthenticatedUser createUser(
            String userKey,
            String email,
            String normalizedEmail,
            String passwordHash,
            String locale,
            String timezone,
            Instant createdAt);

    void assignRole(long userId, String roleCode);

    boolean hasRole(String roleCode);

    void updateLastLogin(long userId, Instant lastLoginAt);
}
