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

    private static String ownerKey(UserKey userKey, String sessionId, String key) {
        return userKey.value() + "|" + sessionId + "|" + key;
    }

    private record StoredAttempt(UserKey owner, LessonAttempt attempt) {
    }
}
