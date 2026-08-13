package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.TrainingSessionCompletion;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.reporting.DailySummaryEvidence;
import cn.forever24.tutor.reporting.DailyTrainingSummary;
import cn.forever24.tutor.reporting.DailyTrainingSummaryGenerator;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTrainingSessionRepository implements TrainingSessionRepository {

    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicLong attemptSequence = new AtomicLong(0);
    private final Map<String, TrainingSession> sessionsByOwnerAndId = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();
    private final Map<String, StoredAttempt> attemptsByIdempotency = new ConcurrentHashMap<>();
    private final Map<String, StoredAttempt> attemptsBySessionTask = new ConcurrentHashMap<>();
    private final Map<String, DailyTrainingSummary> summariesBySession = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryTrainingSessionRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TrainingSession startDailySession(
            UserKey userKey,
            LearningPlan plan,
            TrainingSessionMode mode,
            String idempotencyKey
    ) {
        String idempotencyScope = scoped(userKey, idempotencyKey);
        String existingSessionId = idempotencyIndex.get(idempotencyScope);
        if (existingSessionId != null) {
            return findById(userKey, existingSessionId)
                    .orElseThrow(() -> new IllegalStateException("idempotent training session was not found"));
        }
        TrainingSession session = TrainingSession.startDaily(
                "training-" + sequence.incrementAndGet(),
                plan.planId(),
                mode,
                plan.tasks().get(0).taskId(),
                clock.instant());
        sessionsByOwnerAndId.put(scoped(userKey, session.sessionId()), session);
        idempotencyIndex.put(idempotencyScope, session.sessionId());
        return session;
    }

    @Override
    public Optional<TrainingSession> findById(UserKey userKey, String sessionId) {
        return Optional.ofNullable(sessionsByOwnerAndId.get(scoped(userKey, sessionId)));
    }

    @Override
    public TrainingSession save(UserKey userKey, TrainingSession session) {
        sessionsByOwnerAndId.put(scoped(userKey, session.sessionId()), session);
        return session;
    }

    @Override
    public TrainingSessionCompletion completeSession(UserKey userKey, TrainingSession session) {
        String sessionScope = scoped(userKey, session.sessionId());
        DailyTrainingSummary existing = summariesBySession.get(sessionScope);
        if (existing != null) {
            return new TrainingSessionCompletion(sessionsByOwnerAndId.get(sessionScope), existing);
        }
        List<StoredAttempt> attempts = attemptsBySessionTask.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionScope + ":"))
                .map(Map.Entry::getValue)
                .toList();
        List<DailySummaryEvidence> evidence = attempts.stream()
                .flatMap(attempt -> attempt.evidence().stream())
                .toList();
        DailyTrainingSummary summary = DailyTrainingSummaryGenerator.generate(
                session.sessionId(),
                attempts.size(),
                evidence,
                session.completedAt() == null ? clock.instant() : session.completedAt());
        sessionsByOwnerAndId.put(sessionScope, session);
        summariesBySession.put(sessionScope, summary);
        return new TrainingSessionCompletion(session, summary);
    }

    @Override
    public TaskAttemptReceipt submitTextAttempt(
            UserKey userKey,
            TrainingSession session,
            LearningPlanTask task,
            TaskAttemptSubmission submission,
            List<LearningEvidenceDraft> evidence,
            String idempotencyKey,
            String nextTaskId
    ) {
        String idempotencyScope = scoped(userKey, session.sessionId() + ":" + task.taskId() + ":" + idempotencyKey);
        StoredAttempt existing = attemptsByIdempotency.get(idempotencyScope);
        if (existing != null) {
            existing.requireSame(submission);
            return TaskAttemptReceipt.accepted(existing.attemptId(), existing.evidenceCount());
        }
        if (!session.currentTaskId().equals(task.taskId())) {
            throw new IllegalArgumentException("task is not the current training task");
        }
        String sessionTaskScope = scoped(userKey, session.sessionId() + ":" + task.taskId());
        if (attemptsBySessionTask.containsKey(sessionTaskScope)) {
            throw new IllegalArgumentException("training task already has an accepted attempt");
        }
        StoredAttempt attempt = new StoredAttempt(
                "attempt-" + attemptSequence.incrementAndGet(),
                submission.textHash(),
                submission.hintLevel(),
                evidence == null ? List.of() : evidence.stream()
                        .map(item -> new DailySummaryEvidence(
                                item.skillDimension(),
                                item.evidenceType().name(),
                                item.result().name(),
                                item.knowledgeKey()))
                        .toList());
        attemptsByIdempotency.put(idempotencyScope, attempt);
        attemptsBySessionTask.put(sessionTaskScope, attempt);
        if (nextTaskId != null) {
            sessionsByOwnerAndId.put(scoped(userKey, session.sessionId()), session.moveToTask(nextTaskId));
        }
        return TaskAttemptReceipt.accepted(attempt.attemptId(), attempt.evidenceCount());
    }

    private static String scoped(UserKey userKey, String value) {
        return userKey.value() + ":" + value;
    }

    private record StoredAttempt(
            String attemptId,
            String textHash,
            int hintLevel,
            List<DailySummaryEvidence> evidence
    ) {

        private int evidenceCount() {
            return evidence.size();
        }

        private void requireSame(TaskAttemptSubmission submission) {
            if (!textHash.equals(submission.textHash()) || hintLevel != submission.hintLevel()) {
                throw new IllegalArgumentException("Idempotency-Key was reused with a different request");
            }
        }
    }
}
