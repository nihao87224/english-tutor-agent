package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.training.TrainingSessionCompletion;
import cn.forever24.tutor.application.training.TrainingSessionApplicationService;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.reporting.DailySummaryEvidence;
import cn.forever24.tutor.reporting.DailyTrainingSummary;
import cn.forever24.tutor.reporting.DailyTrainingSummaryGenerator;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainingSessionControllerTest {

    private final TrainingSessionController controller = new TrainingSessionController(
            new TrainingSessionApplicationService(
                    new ReadyProfileRepository(),
                    new FakeLearningPlanRepository(),
                    new FakeTrainingSessionRepository(),
                    Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneOffset.UTC)),
            CurrentUserKeyResolver.legacyOnly());

    @Test
    void startsAndMovesTrainingSessionThroughLifecycle() {
        ResponseEntity<TrainingSessionResponse> started = controller.startTrainingSession(
                "user-1",
                "idem-1",
                new StartTrainingSessionRequest("plan-1", "TEXT"));

        String sessionId = started.getBody().sessionId();
        TrainingSessionResponse current = controller.getTrainingSession("user-1", sessionId);
        CurrentTrainingTaskResponse currentTask = controller.getCurrentTask("user-1", sessionId);
        TrainingSessionResponse paused = controller.pauseTrainingSession("user-1", sessionId);
        TrainingSessionResponse resumed = controller.resumeTrainingSession("user-1", sessionId);
        controller.submitTaskAttempt(
                "user-1",
                "attempt-idem-complete",
                sessionId,
                "task-1",
                new TaskAttemptRequest(
                        "TEXT",
                        "I think it was caused by an unstable connection.",
                        null,
                        null,
                        1,
                        1200,
                        null,
                        null));
        TrainingSessionCompletionResponse completed = controller.completeTrainingSession("user-1", sessionId);

        assertEquals(201, started.getStatusCode().value());
        assertEquals("IN_PROGRESS", current.status());
        assertEquals("task-1", currentTask.taskId());
        assertEquals("STARTED", currentTask.status());
        assertEquals("PAUSED", paused.status());
        assertEquals("IN_PROGRESS", resumed.status());
        assertEquals("COMPLETED", completed.session().status());
        assertEquals(1, completed.dailySummary().evidenceCount());
    }

    @Test
    void submitsTextTaskAttempt() {
        ResponseEntity<TrainingSessionResponse> started = controller.startTrainingSession(
                "user-1",
                "idem-1",
                new StartTrainingSessionRequest("plan-1", "TEXT"));

        ResponseEntity<TaskAttemptReceiptResponse> receipt = controller.submitTaskAttempt(
                "user-1",
                "attempt-idem-1",
                started.getBody().sessionId(),
                "task-1",
                new TaskAttemptRequest(
                        "TEXT",
                        "I think it was caused by an unstable connection.",
                        null,
                        null,
                        1,
                        1200,
                        null,
                        null));

        assertEquals(202, receipt.getStatusCode().value());
        assertEquals("attempt-1", receipt.getBody().attemptId());
        assertEquals("ACCEPTED", receipt.getBody().status());
        assertEquals(false, receipt.getBody().feedbackAvailable());
        assertEquals(1, receipt.getBody().evidenceCount());
    }

    @Test
    void invalidTrainingRequestMapsToBadRequestProblem() {
        ResponseEntity<?> response = controller.handleBadRequest(new IllegalArgumentException("not ready"));

        assertEquals(400, response.getStatusCode().value());
    }

    private static final class FakeTrainingSessionRepository implements TrainingSessionRepository {

        private final ConcurrentHashMap<String, TrainingSession> sessions = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> attempts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<DailySummaryEvidence>> evidenceBySession = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, TrainingSessionCompletion> completions = new ConcurrentHashMap<>();

        @Override
        public TrainingSession startDailySession(
                UserKey userKey,
                LearningPlan plan,
                TrainingSessionMode mode,
                String idempotencyKey
        ) {
            return sessions.computeIfAbsent(userKey.value() + ":training-1", ignored -> TrainingSession.startDaily(
                    "training-1",
                    plan.planId(),
                    mode,
                    plan.tasks().get(0).taskId(),
                    Instant.parse("2026-08-10T08:00:00Z")));
        }

        @Override
        public Optional<TrainingSession> findById(UserKey userKey, String sessionId) {
            return Optional.ofNullable(sessions.get(userKey.value() + ":" + sessionId));
        }

        @Override
        public TrainingSession save(UserKey userKey, TrainingSession session) {
            sessions.put(userKey.value() + ":" + session.sessionId(), session);
            return session;
        }

        @Override
        public TrainingSessionCompletion completeSession(UserKey userKey, TrainingSession session) {
            String sessionScope = userKey.value() + ":" + session.sessionId();
            TrainingSessionCompletion existing = completions.get(sessionScope);
            if (existing != null) {
                return existing;
            }
            List<DailySummaryEvidence> evidence = evidenceBySession.getOrDefault(sessionScope, List.of());
            DailyTrainingSummary summary = DailyTrainingSummaryGenerator.generate(
                    session.sessionId(),
                    evidence.isEmpty() ? 0 : 1,
                    evidence,
                    session.completedAt() == null ? Instant.parse("2026-08-10T08:00:00Z") : session.completedAt());
            TrainingSessionCompletion completion = new TrainingSessionCompletion(session, summary);
            sessions.put(sessionScope, session);
            completions.put(sessionScope, completion);
            return completion;
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
            String attemptId = attempts.computeIfAbsent(
                    userKey.value() + ":" + session.sessionId() + ":" + task.taskId() + ":" + idempotencyKey,
                    ignored -> "attempt-" + (attempts.size() + 1));
            evidenceBySession.put(userKey.value() + ":" + session.sessionId(), toSummaryEvidence(evidence));
            return TaskAttemptReceipt.accepted(attemptId, evidence == null ? 0 : evidence.size());
        }

        private List<DailySummaryEvidence> toSummaryEvidence(List<LearningEvidenceDraft> evidence) {
            if (evidence == null) {
                return List.of();
            }
            return evidence.stream()
                    .map(item -> new DailySummaryEvidence(
                            item.skillDimension(),
                            item.evidenceType().name(),
                            item.result().name(),
                            item.knowledgeKey()))
                    .toList();
        }
    }

    private static final class FakeLearningPlanRepository implements LearningPlanRepository {

        @Override
        public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LearningPlan getPlan(UserKey userKey, String planId) {
            return new LearningPlan(
                    "plan-1",
                    LocalDate.parse("2026-08-10"),
                    10,
                    List.of(new LearningPlanTask(
                            "task-1",
                            "SPEAKING",
                            "Practice a status update",
                            10,
                            List.of("speaking"),
                            "A2",
                            "Workplace speaking is the weakest skill.")),
                    List.of("Focus on speaking today."),
                    false,
                    1);
        }

        @Override
        public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
        }
    }

    private static final class ReadyProfileRepository implements UserProfileRepository {

        @Override
        public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PrivacySettings getPrivacySettings(UserKey userKey) {
            return new PrivacySettings(
                    cn.forever24.tutor.profile.RawContentRetention.STORE,
                    cn.forever24.tutor.profile.RawContentRetention.STORE,
                    30);
        }

        @Override
        public PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void advanceOnboardingToAssessment(UserKey userKey) {
        }

        @Override
        public void advanceOnboardingToResult(UserKey userKey) {
        }

        @Override
        public OnboardingProgress getOnboardingProgress(UserKey userKey) {
            return new OnboardingProgress(OnboardingStep.RESULT, false, null);
        }
    }
}
