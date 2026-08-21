package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;

import java.util.List;
import java.util.Optional;

public interface LessonAttemptRepository {
    Optional<LessonAttemptStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String idempotencyKey);
    Optional<LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId);
    List<LessonAttempt> findBySession(UserKey userKey, String sessionId);
    void insert(UserKey userKey, LessonAttempt attempt, String idempotencyKey, String requestHash);
}
