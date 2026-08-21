package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonAttemptRepository;
import cn.forever24.tutor.application.training.LessonAttemptStoreRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLessonAttemptRepository implements LessonAttemptRepository {
    private final Map<String, StoredAttempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, LessonAttemptStoreRecord> idempotency = new ConcurrentHashMap<>();
    private final Map<String, LessonAttemptStoreRecord> confirmations = new ConcurrentHashMap<>();

    @Override
    public Optional<LessonAttemptStoreRecord> findByIdempotencyKey(
            UserKey userKey, String sessionId, String idempotencyKey
    ) {
        return Optional.ofNullable(idempotency.get(ownerKey(userKey, sessionId, idempotencyKey)));
    }

    @Override
    public Optional<LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId) {
        StoredAttempt stored = attempts.get(attemptId);
        return stored != null && stored.owner().equals(userKey) && stored.attempt().sessionId().equals(sessionId)
                ? Optional.of(stored.attempt()) : Optional.empty();
    }

    @Override
    public Optional<LessonAttemptStoreRecord> findByTranscriptConfirmationKey(
            UserKey userKey, String sessionId, String attemptId, String idempotencyKey) {
        return Optional.ofNullable(confirmations.get(ownerKey(userKey, sessionId, attemptId + "|" + idempotencyKey)));
    }

    @Override
    public List<LessonAttempt> findBySession(UserKey userKey, String sessionId) {
        return attempts.values().stream()
                .filter(stored -> stored.owner().equals(userKey) && stored.attempt().sessionId().equals(sessionId))
                .map(StoredAttempt::attempt)
                .sorted(Comparator.comparing(LessonAttempt::submittedAt))
                .toList();
    }

    @Override
    public synchronized void insert(
            UserKey userKey, LessonAttempt attempt, String idempotencyKey, String requestHash
    ) {
        attempts.put(attempt.attemptId(), new StoredAttempt(userKey, attempt));
        idempotency.put(ownerKey(userKey, attempt.sessionId(), idempotencyKey),
                new LessonAttemptStoreRecord(requestHash, attempt));
    }

    @Override
    public synchronized void updateTranscription(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        update(userKey, attempt, expectedVersion);
    }

    @Override
    public synchronized void updateTranscriptConfirmation(
            UserKey userKey, LessonAttempt attempt, long expectedVersion, String idempotencyKey, String requestHash) {
        update(userKey, attempt, expectedVersion);
        confirmations.put(ownerKey(userKey, attempt.sessionId(), attempt.attemptId() + "|" + idempotencyKey),
                new LessonAttemptStoreRecord(requestHash, attempt));
    }

    @Override
    public synchronized void updateAnalysis(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        update(userKey, attempt, expectedVersion);
    }

    private void update(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        StoredAttempt stored = attempts.get(attempt.attemptId());
        if (stored == null || !stored.owner().equals(userKey) || stored.attempt().version() != expectedVersion) {
            throw new IllegalStateException("lesson attempt version conflict");
        }
        attempts.put(attempt.attemptId(), new StoredAttempt(userKey, attempt));
        idempotency.replaceAll((key, record) -> record.attempt().attemptId().equals(attempt.attemptId())
                ? new LessonAttemptStoreRecord(record.requestHash(), attempt) : record);
    }

    private static String ownerKey(UserKey userKey, String sessionId, String key) {
        return userKey.value() + "|" + sessionId + "|" + key;
    }

    private record StoredAttempt(UserKey owner, LessonAttempt attempt) {
    }
}
