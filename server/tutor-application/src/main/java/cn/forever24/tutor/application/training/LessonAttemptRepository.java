package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;

import java.util.List;
import java.util.Optional;

public interface LessonAttemptRepository {
    Optional<LessonAttemptStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String idempotencyKey);
    Optional<LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId);
    default Optional<LessonAttemptStoreRecord> findByTranscriptConfirmationKey(
            UserKey userKey, String sessionId, String attemptId, String idempotencyKey) {
        return Optional.empty();
    }
    List<LessonAttempt> findBySession(UserKey userKey, String sessionId);
    void insert(UserKey userKey, LessonAttempt attempt, String idempotencyKey, String requestHash);
    default void updateTranscription(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        throw new UnsupportedOperationException("transcription updates are not supported");
    }
    default void updateTranscriptConfirmation(
            UserKey userKey, LessonAttempt attempt, long expectedVersion, String idempotencyKey, String requestHash) {
        throw new UnsupportedOperationException("transcript confirmations are not supported");
    }
    default void updateAnalysis(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        throw new UnsupportedOperationException("analysis updates are not supported");
    }
}
