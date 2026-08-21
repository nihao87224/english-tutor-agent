package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import cn.forever24.tutor.application.training.LessonSessionRepository;
import cn.forever24.tutor.application.training.LessonSessionStartRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLessonSessionRepository implements LessonSessionRepository {

    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, LessonSessionStartRecord> starts = new ConcurrentHashMap<>();
    private final Object monitor = new Object();

    @Override
    public Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey) {
        synchronized (monitor) {
            return Optional.ofNullable(starts.get(ownerKey(userKey, idempotencyKey)));
        }
    }

    @Override
    public void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash) {
        synchronized (monitor) {
            sessions.put(session.sessionId(), new StoredSession(userKey, session));
            starts.put(ownerKey(userKey, idempotencyKey), new LessonSessionStartRecord(requestHash, session));
        }
    }

    @Override
    public Optional<LessonSession> findById(UserKey userKey, String sessionId) {
        StoredSession stored = sessions.get(sessionId);
        return stored != null && stored.owner().equals(userKey)
                ? Optional.of(stored.session()) : Optional.empty();
    }

    @Override
    public Optional<LessonSession> findByIdForUpdate(UserKey userKey, String sessionId) {
        synchronized (monitor) {
            return findById(userKey, sessionId);
        }
    }

    @Override
    public LessonSession save(UserKey userKey, long expectedVersion, LessonSession session) {
        synchronized (monitor) {
            StoredSession stored = sessions.get(session.sessionId());
            if (stored == null || !stored.owner().equals(userKey)) {
                throw LessonSessionApplicationException.notFound();
            }
            if (stored.session().version() != expectedVersion) {
                throw LessonSessionApplicationException.versionConflict();
            }
            sessions.put(session.sessionId(), new StoredSession(userKey, session));
            starts.replaceAll((key, record) -> record.session().sessionId().equals(session.sessionId())
                    ? new LessonSessionStartRecord(record.requestHash(), session) : record);
            return session;
        }
    }

    private static String ownerKey(UserKey userKey, String idempotencyKey) {
        return userKey.value() + "|START|" + idempotencyKey;
    }

    private record StoredSession(UserKey owner, LessonSession session) {
    }
}
