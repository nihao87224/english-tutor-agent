package cn.forever24.tutor.application.auth;

import java.time.Instant;
import java.util.Optional;

public interface RefreshSessionRepository {

    void create(RefreshSessionDraft session);

    Optional<StoredRefreshSession> findByTokenHash(String tokenHash);

    void revoke(String sessionId, Instant revokedAt);

    void revokeAndReplace(String sessionId, String replacementSessionId, Instant revokedAt);
}
