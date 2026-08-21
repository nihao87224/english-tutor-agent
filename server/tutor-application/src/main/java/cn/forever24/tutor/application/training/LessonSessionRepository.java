package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonSession;

import java.util.Optional;

public interface LessonSessionRepository {

    Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey);

    void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash);

    Optional<LessonSession> findById(UserKey userKey, String sessionId);

    default Optional<LessonSession> findByIdForUpdate(UserKey userKey, String sessionId) {
        return findById(userKey, sessionId);
    }

    LessonSession save(UserKey userKey, long expectedVersion, LessonSession session);
}
