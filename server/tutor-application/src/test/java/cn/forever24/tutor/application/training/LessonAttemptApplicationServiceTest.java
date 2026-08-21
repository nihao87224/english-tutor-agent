package cn.forever24.tutor.application.training;

import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.ObjectiveAnswerScorer;
import cn.forever24.tutor.training.TaskAttemptInputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonAttemptApplicationServiceTest {
    private static final UserKey USER = new UserKey("usr-1");
    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAttemptRepository attempts = new FakeAttemptRepository();
    private LessonAttemptApplicationService service;

    @BeforeEach
    void setUp() {
        AtomicInteger sequence = new AtomicInteger();
        service = new LessonAttemptApplicationService(
                sessions, attempts, (resource, version) -> content(),
                new DirectTransactions(),
                () -> "lat-" + sequence.incrementAndGet(), new ObjectiveAnswerScorer(),
                Clock.fixed(Instant.parse("2026-08-21T01:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void scoresEveryQuestionDeterministicallyAndAdvancesOnlyAfterAllChecks() {
        seed(comprehensionSession());

        var first = service.submit("usr-1", "lsn-1", command("q1", "Gate 24"), "idem-1");
        assertEquals(LessonAttemptStatus.ANALYZED, first.attempt().status());
        assertTrue(first.attempt().objectiveResult().correct());
        assertEquals(List.of("q2"), first.progress().remainingTaskIds());
        assertEquals(LessonStep.COMPREHENSION, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());

        var second = service.submit("usr-1", "lsn-1", command("q2", "4:00"), "idem-2");
        assertFalse(second.attempt().objectiveResult().correct());
        assertTrue(second.progress().nextStepEligible());
        assertEquals(LessonStep.TRANSCRIPT_EXPRESSIONS,
                sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
    }

    @Test
    void replaysSameRequestAfterStepAdvancedAndRejectsChangedPayload() {
        seed(comprehensionSession());
        var first = service.submit("usr-1", "lsn-1", command("q1", "Gate 24"), "same-key");
        var replay = service.submit("usr-1", "lsn-1", command("q1", "Gate 24"), "same-key");

        assertTrue(replay.replayed());
        assertEquals(first.attempt().attemptId(), replay.attempt().attemptId());
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(LessonSessionApplicationException.class,
                () -> service.submit("usr-1", "lsn-1", command("q1", "Gate 25"), "same-key")).code());
    }

    @Test
    void validatesTaskOwnershipAndPersistsGuidedTextBeforeAdvancing() {
        seed(guidedSession());
        assertEquals("SESSION_STATE_CONFLICT", assertThrows(LessonSessionApplicationException.class,
                () -> service.submit("usr-1", "lsn-1", command("other", "Hello"), "bad-task")).code());

        var result = service.submit("usr-1", "lsn-1", command("guided-1", "Gate 24. Boarding is at 3:20."), "guided");
        assertEquals(LessonAttemptStatus.ANALYSIS_PENDING, result.attempt().status());
        assertEquals(LessonStep.ROLE_PLAY, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
        assertEquals("SESSION_NOT_FOUND", assertThrows(LessonSessionApplicationException.class,
                () -> service.get("usr-2", "lsn-1", result.attempt().attemptId())).code());
    }

    private void seed(LessonSession session) {
        sessions.insert(USER, session, "start", "hash");
    }

    private static LessonSession comprehensionSession() {
        return base().completeDeterministicStep(LessonStep.SCENE_CONTEXT)
                .completeDeterministicStep(LessonStep.FIRST_LISTEN);
    }

    private static LessonSession guidedSession() {
        return comprehensionSession().completeAttemptStep(LessonStep.COMPREHENSION)
                .completeDeterministicStep(LessonStep.TRANSCRIPT_EXPRESSIONS);
    }

    private static LessonSession base() {
        return LessonSession.start(
                "lsn-1", "prx-1", 1, "block-1", "resource-1", "1.0.0", "skill-1", "mapping-1",
                TrainingType.ROLE_PLAY, LessonInputMode.VOICE_OR_TEXT, Instant.parse("2026-08-21T00:00:00Z"));
    }

    private static LessonContent content() {
        return new LessonContent(
                List.of(new ComprehensionQuestion("q1", "Gate?", "Gate 24"),
                        new ComprehensionQuestion("q2", "Time?", "At 3:20")),
                List.of(new GuidedSpeakingTask("guided-1", "Tell Lin Muen", List.of("gate"), List.of("Your flight..."))));
    }

    private static SubmitLessonAttemptCommand command(String taskId, String text) {
        return new SubmitLessonAttemptCommand(taskId, TaskAttemptInputType.TEXT, text, null, null, null);
    }

    private static final class DirectTransactions implements LessonSessionTransactionOperations {
        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }

    private static final class FakeSessionRepository implements LessonSessionRepository {
        private final Map<String, LessonSession> values = new HashMap<>();
        private UserKey owner;

        @Override
        public Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash) {
            owner = userKey;
            values.put(session.sessionId(), session);
        }

        @Override
        public Optional<LessonSession> findById(UserKey userKey, String sessionId) {
            return userKey.equals(owner) ? Optional.ofNullable(values.get(sessionId)) : Optional.empty();
        }

        @Override
        public Optional<LessonSession> findByIdForUpdate(UserKey userKey, String sessionId) {
            return findById(userKey, sessionId);
        }

        @Override
        public LessonSession save(UserKey userKey, long expectedVersion, LessonSession session) {
            values.put(session.sessionId(), session);
            return session;
        }
    }

    private static final class FakeAttemptRepository implements LessonAttemptRepository {
        private final List<cn.forever24.tutor.training.LessonAttempt> values = new ArrayList<>();
        private final Map<String, LessonAttemptStoreRecord> idempotency = new HashMap<>();

        @Override
        public Optional<LessonAttemptStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String key) {
            return Optional.ofNullable(idempotency.get(userKey.value() + sessionId + key));
        }

        @Override
        public Optional<cn.forever24.tutor.training.LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId) {
            if (!USER.equals(userKey)) return Optional.empty();
            return values.stream().filter(value -> value.sessionId().equals(sessionId)
                    && value.attemptId().equals(attemptId)).findFirst();
        }

        @Override
        public List<cn.forever24.tutor.training.LessonAttempt> findBySession(UserKey userKey, String sessionId) {
            if (!USER.equals(userKey)) return List.of();
            return values.stream().filter(value -> value.sessionId().equals(sessionId)).toList();
        }

        @Override
        public void insert(UserKey userKey, cn.forever24.tutor.training.LessonAttempt attempt,
                           String idempotencyKey, String requestHash) {
            values.add(attempt);
            idempotency.put(userKey.value() + attempt.sessionId() + idempotencyKey,
                    new LessonAttemptStoreRecord(requestHash, attempt));
        }
    }
}
