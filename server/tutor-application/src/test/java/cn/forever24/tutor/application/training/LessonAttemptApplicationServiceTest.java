package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.audio.*;
import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.ObjectiveAnswerScorer;
import cn.forever24.tutor.training.TaskAttemptInputType;
import cn.forever24.tutor.training.AttemptAnalysis;
import cn.forever24.tutor.training.AttemptCriterionResult;
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
import java.util.Map;
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
        service = audioService(new FakeAudioRepository(), request -> new AudioTranscription("unused", 1.0));
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
        assertEquals(LessonStep.GUIDED_SPEAKING, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
        assertEquals("SESSION_NOT_FOUND", assertThrows(LessonSessionApplicationException.class,
                () -> service.get("usr-2", "lsn-1", result.attempt().attemptId())).code());
    }

    @Test
    void lowConfidenceAudioRequiresConfirmationBeforeAdvancing() {
        seed(guidedSession());
        FakeAudioRepository audio = new FakeAudioRepository();
        audio.asset = audioAsset();
        service = audioService(audio, request -> new AudioTranscription("Gate twenty four", 0.42));

        var received = service.submit("usr-1", "lsn-1", audioCommand(), "audio-attempt");
        assertEquals(LessonAttemptStatus.RECEIVED, received.attempt().status());
        assertFalse(received.attempt().transcriptConfirmed());
        assertEquals(LessonStep.GUIDED_SPEAKING, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());

        var confirmed = service.confirmTranscript("usr-1", "lsn-1", received.attempt().attemptId(),
                new ConfirmTranscriptCommand(TranscriptConfirmationDecision.CORRECT, "Gate 24"), "confirm-1");
        assertTrue(confirmed.attempt().transcriptConfirmed());
        assertEquals("Gate 24", confirmed.attempt().transcript());
        assertEquals(LessonStep.GUIDED_SPEAKING, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
        assertTrue(service.confirmTranscript("usr-1", "lsn-1", received.attempt().attemptId(),
                new ConfirmTranscriptCommand(TranscriptConfirmationDecision.CORRECT, "Gate 24"), "confirm-1").replayed());
    }

    @Test
    void providerTimeoutIsRetryableAndRerecordDoesNotAdvance() {
        seed(guidedSession());
        FakeAudioRepository audio = new FakeAudioRepository();
        audio.asset = audioAsset();
        service = audioService(audio, request -> { throw new AudioTranscriptionException(true, "timeout", null); });
        var timedOut = service.submit("usr-1", "lsn-1", audioCommand(), "timeout");
        assertEquals(LessonAttemptStatus.TRANSCRIPTION_RETRYABLE, timedOut.attempt().status());
        assertEquals(LessonStep.GUIDED_SPEAKING, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());

        service = audioService(audio, request -> new AudioTranscription("Gate twenty four", 0.20));
        var low = service.submit("usr-1", "lsn-1", audioCommand(), "low");
        var rerecord = service.confirmTranscript("usr-1", "lsn-1", low.attempt().attemptId(),
                new ConfirmTranscriptCommand(TranscriptConfirmationDecision.RE_RECORD, null), "rerecord");
        assertEquals(LessonAttemptStatus.RETRY_REQUIRED, rerecord.attempt().status());
        assertEquals(LessonStep.GUIDED_SPEAKING, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
    }

    @Test
    void rejectsAudioAssetOwnedByAnotherUser() {
        seed(guidedSession());
        service = audioService(new FakeAudioRepository(), request -> new AudioTranscription("hello", 0.9));
        assertEquals("AUDIO_ASSET_NOT_FOUND", assertThrows(LessonSessionApplicationException.class,
                () -> service.submit("usr-1", "lsn-1", audioCommand(), "foreign")).code());
    }

    @Test
    void acceptsOnlyValidatedAnalysisAndAdvancesTheSpeakingStep() {
        seed(guidedSession());
        service = analyzedService(context -> new AttemptAnalysis("Clear response.", List.of(
                new AttemptCriterionResult("guided-1:criterion:1", true, "The gate is clear.")), List.of(),
                List.of("The gate is now 24."), "test-prompt", "test", "test", "trace-1"));

        var result = service.submit("usr-1", "lsn-1", command("guided-1", "The gate is 24."), "analysis-ok");

        assertEquals(LessonAttemptStatus.ACCEPTED, result.attempt().status());
        assertEquals(LessonStep.ROLE_PLAY, sessions.findById(USER, "lsn-1").orElseThrow().currentStep());
        assertEquals("Clear response.", result.attempt().analysis().summary());
    }

    @Test
    void rejectsProviderCriteriaOutsideTheLockedLesson() {
        seed(guidedSession());
        service = analyzedService(context -> new AttemptAnalysis("Wrong rubric.", List.of(
                new AttemptCriterionResult("other", true, "No.")), List.of(), List.of(),
                "test-prompt", "test", "test", "trace-2"));

        var result = service.submit("usr-1", "lsn-1", command("guided-1", "The gate is 24."), "analysis-invalid");

        assertEquals(LessonAttemptStatus.ANALYSIS_FAILED, result.attempt().status());
        assertEquals("AI_OUTPUT_INVALID", result.attempt().analysisErrorCode());
    }

    private LessonAttemptApplicationService audioService(AudioAssetRepository audio, AudioTranscriber transcriber) {
        return new LessonAttemptApplicationService(sessions, attempts, (resource, version) -> content(),
                new DirectTransactions(), () -> "lat-" + (attempts.values.size() + 1), new ObjectiveAnswerScorer(),
                audio, new PrivateAudioObjectStorage() {
                    public void put(String key, byte[] content) { }
                    public byte[] read(String key) { return new byte[]{1, 2, 3}; }
                    public void delete(String key) { }
                }, transcriber, 0.8, Clock.fixed(Instant.parse("2026-08-21T01:00:00Z"), ZoneOffset.UTC));
    }

    private LessonAttemptApplicationService analyzedService(SpeakingAttemptAnalyzer analyzer) {
        return new LessonAttemptApplicationService(sessions, attempts, (resource, version) -> content(),
                new DirectTransactions(), () -> "lat-" + (attempts.values.size() + 1), new ObjectiveAnswerScorer(),
                new FakeAudioRepository(), new PrivateAudioObjectStorage() {
                    public void put(String key, byte[] content) { }
                    public byte[] read(String key) { return new byte[]{1}; }
                    public void delete(String key) { }
                }, request -> new AudioTranscription("unused", 1.0), 0.8, analyzer, Clock.fixed(
                        Instant.parse("2026-08-21T01:00:00Z"), ZoneOffset.UTC));
    }

    private static SubmitLessonAttemptCommand audioCommand() {
        return new SubmitLessonAttemptCommand(
                "guided-1", TaskAttemptInputType.AUDIO, null, "usr_audio_1", null, null, 1_000);
    }

    private static UserAudioAsset audioAsset() {
        return new UserAudioAsset("usr_audio_1", "private/1.webm", "LESSON_ATTEMPT", "audio/webm",
                3, 1_000, "sha256:abc", AudioAssetStatus.READY, RawContentRetention.STORE, null,
                Instant.parse("2026-08-21T00:00:00Z"));
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
        return new SubmitLessonAttemptCommand(taskId, TaskAttemptInputType.TEXT, text, null, null, null, null);
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
        private final Map<String, LessonAttemptStoreRecord> confirmations = new HashMap<>();

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
        public Optional<LessonAttemptStoreRecord> findByTranscriptConfirmationKey(
                UserKey userKey, String sessionId, String attemptId, String key) {
            return Optional.ofNullable(confirmations.get(userKey.value() + sessionId + attemptId + key));
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

        @Override
        public void updateTranscription(UserKey userKey, cn.forever24.tutor.training.LessonAttempt attempt, long expectedVersion) {
            replace(attempt, expectedVersion);
        }

        @Override
        public void updateTranscriptConfirmation(UserKey userKey, cn.forever24.tutor.training.LessonAttempt attempt,
                                                 long expectedVersion, String key, String hash) {
            replace(attempt, expectedVersion);
            confirmations.put(userKey.value() + attempt.sessionId() + attempt.attemptId() + key,
                    new LessonAttemptStoreRecord(hash, attempt));
        }

        @Override
        public void updateAnalysis(UserKey userKey, cn.forever24.tutor.training.LessonAttempt attempt, long expectedVersion) {
            replace(attempt, expectedVersion);
        }

        private void replace(cn.forever24.tutor.training.LessonAttempt attempt, long expectedVersion) {
            var current = values.stream().filter(value -> value.attemptId().equals(attempt.attemptId())).findFirst().orElseThrow();
            assertEquals(expectedVersion, current.version());
            values.remove(current);
            values.add(attempt);
            idempotency.replaceAll((key, record) -> record.attempt().attemptId().equals(attempt.attemptId())
                    ? new LessonAttemptStoreRecord(record.requestHash(), attempt) : record);
        }
    }

    private static final class FakeAudioRepository implements AudioAssetRepository {
        private UserAudioAsset asset;
        public Optional<AudioAssetStoreRecord> findByIdempotencyKey(UserKey userKey, String key) { return Optional.empty(); }
        public Optional<UserAudioAsset> findById(UserKey userKey, String id) {
            return USER.equals(userKey) && asset != null && asset.audioAssetId().equals(id) ? Optional.of(asset) : Optional.empty();
        }
        public void insert(UserKey userKey, UserAudioAsset value, String key, String hash) { asset = value; }
        public void markDeleted(UserKey userKey, String id) { }
        public List<OwnedAudioAsset> findExpired(Instant now, int limit) { return List.of(); }
    }
}
