package cn.forever24.tutor.infrastructure.auth;

import cn.forever24.tutor.application.auth.RefreshSessionDraft;
import cn.forever24.tutor.application.auth.RefreshSessionRepository;
import cn.forever24.tutor.application.auth.StoredRefreshSession;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefreshSessionRepository implements RefreshSessionRepository {

    private final Map<String, StoredRefreshSession> byId = new ConcurrentHashMap<>();
    private final Map<String, String> idByHash = new ConcurrentHashMap<>();

    @Override
    public void create(RefreshSessionDraft session) {
        StoredRefreshSession stored = new StoredRefreshSession(
                session.id(),
                session.userId(),
                session.tokenHash(),
                session.authVersion(),
                session.expiresAt(),
                null);
        byId.put(session.id(), stored);
        idByHash.put(session.tokenHash(), session.id());
    }

    @Override
    public Optional<StoredRefreshSession> findByTokenHash(String tokenHash) {
        String id = idByHash.get(tokenHash);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public void revoke(String sessionId, Instant revokedAt) {
        byId.computeIfPresent(sessionId, (ignored, session) -> new StoredRefreshSession(
                session.id(), session.userId(), session.tokenHash(), session.authVersion(), session.expiresAt(), revokedAt));
    }

    @Override
    public void revokeAndReplace(String sessionId, String replacementSessionId, Instant revokedAt) {
        revoke(sessionId, revokedAt);
    }
}
