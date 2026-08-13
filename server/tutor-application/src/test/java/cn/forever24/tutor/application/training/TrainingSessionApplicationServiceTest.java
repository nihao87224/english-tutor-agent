package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.reporting.DailySummaryEvidence;
import cn.forever24.tutor.reporting.DailyTrainingSummary;
import cn.forever24.tutor.reporting.DailyTrainingSummaryGenerator;
import cn.forever24.tutor.training.CurrentTrainingTask;
import cn.forever24.tutor.training.TaskAttemptInputType;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import cn.forever24.tutor.training.TrainingSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingSessionApplicationServiceTest {

    private static final LearningPlan PLAN = new LearningPlan(
            "plan-1",
            LocalDate.parse("2026-08-10"),
            15,
            List.of(new LearningPlanTask(
                    "task-1",
                    "SPEAKING",
                    "Practice a status update",
                    10,
                    List.of("speaking"),
                    "A2",
                    "Workplace speaking is the weakest skill."),
                    new LearningPlanTask(
                            "task-2",
                            "WRITING",
                            "Rewrite a short sentence",
                            5,
                            List.of("writing"),
                            "A2",
                            "Writing needs a small follow-up task.")),
            List.of("Focus on speaking today."),
            false,
            1);

    private final FakeUserProfileRepository userProfileRepository = new FakeUserProfileRepository();
    private final FakeLearningPlanRepository learningPlanRepository = new FakeLearningPlanRepository();
    private final FakeTrainingSessionRepository trainingSessionRepository = new FakeTrainingSessionRepository();
    private final TrainingSessionApplicationService service = new TrainingSessionApplicationService(
            userProfileRepository,
            learningPlanRepository,
            trainingSessionRepository,
            Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void startsDailyTrainingSessionAndReturnsCurrentTask() {
        TrainingSession session = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");
        CurrentTrainingTask currentTask = service.getCurrentTask("user-1", session.sessionId());

        assertEquals(TrainingSessionStatus.IN_PROGRESS, session.status());
        assertEquals("plan-1", session.planId());
        assertEquals("task-1", currentTask.task().taskId());
        assertEquals("STARTED", currentTask.status().name());
    }

    @Test
    void duplicateStartReturnsOriginalSession() {
        TrainingSession first = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");
        TrainingSession repeated = service.startDailySession("user-1", "plan-1", TrainingSessionMode.MIXED, "idem-1");

        assertEquals(first.sessionId(), repeated.sessionId());
        assertEquals(TrainingSessionMode.TEXT, repeated.mode());
    }

    @Test
    void pauseResumeAndCompleteUseRepositoryState() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.MIXED, "idem-1");
        TrainingSession paused = service.pause("user-1", started.sessionId());
        TrainingSession resumed = service.resume("user-1", started.sessionId());
        service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "I think the delay was caused by network instability.",
                1,
                1200,
                null,
                null,
                "attempt-idem-complete");
        TrainingSessionCompletion completed = service.complete("user-1", started.sessionId());
        TrainingSessionCompletion repeated = service.complete("user-1", started.sessionId());

        assertEquals(TrainingSessionStatus.PAUSED, paused.status());
        assertEquals(TrainingSessionStatus.IN_PROGRESS, resumed.status());
        assertEquals(TrainingSessionStatus.COMPLETED, completed.session().status());
        assertEquals(1, completed.dailySummary().evidenceCount());
        assertEquals(completed.dailySummary(), repeated.dailySummary());
        assertEquals(1, learningPlanRepository.completionRecordCount);
        assertThrows(IllegalArgumentException.class, () -> service.getCurrentTask("user-1", started.sessionId()));
    }

    @Test
    void rejectsCompletionWithoutAcceptedAttempt() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        assertThrows(IllegalArgumentException.class, () -> service.complete("user-1", started.sessionId()));
    }

    @Test
    void rejectsUsersBeforeAssessmentResult() {
        userProfileRepository.step = OnboardingStep.ASSESSMENT;

        assertThrows(IllegalArgumentException.class,
                () -> service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1"));
    }

    @Test
    void rejectsOtherUsersSessionAccess() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        assertThrows(IllegalArgumentException.class, () -> service.getSession("user-2", started.sessionId()));
    }

    @Test
    void submitsCurrentTextTaskAndAdvancesToNextTask() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        TaskAttemptReceipt receipt = service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "I think the delay was caused by network instability.",
                1,
                1200,
                null,
                null,
                "attempt-idem-1");
        CurrentTrainingTask currentTask = service.getCurrentTask("user-1", started.sessionId());

        assertEquals("attempt-1", receipt.attemptId());
        assertEquals("ACCEPTED", receipt.status().name());
        assertEquals(1, receipt.evidenceCount());
        assertEquals("task-2", currentTask.task().taskId());
        assertEquals(1, trainingSessionRepository.lastEvidence.size());
    }

    @Test
    void duplicateAttemptSubmissionReturnsOriginalReceipt() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        TaskAttemptReceipt first = service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "I think the delay was caused by network instability.",
                1,
                1200,
                null,
                null,
                "attempt-idem-1");
        TaskAttemptReceipt repeated = service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "I think the delay was caused by network instability.",
                1,
                1200,
                null,
                null,
                "attempt-idem-1");

        assertEquals(first.attemptId(), repeated.attemptId());
    }

    @Test
    void rejectsInvalidAttemptSubmissionStateAndTask() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        assertThrows(IllegalArgumentException.class, () -> service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-2",
                TaskAttemptInputType.TEXT,
                "Not current.",
                0,
                null,
                null,
                null,
                "attempt-idem-1"));

        service.pause("user-1", started.sessionId());
        assertThrows(IllegalArgumentException.class, () -> service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "Paused.",
                0,
                null,
                null,
                null,
                "attempt-idem-2"));
    }

    @Test
    void rejectsUnsupportedAttemptType() {
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        assertThrows(IllegalArgumentException.class, () -> service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.AUDIO,
                null,
                0,
                null,
                null,
                null,
                "attempt-idem-1"));
    }

    @Test
    void doesNotStoreRawTextWhenPrivacyDisablesRetention() {
        userProfileRepository.rawTextRetention = RawContentRetention.PROCESS_ONLY;
        TrainingSession started = service.startDailySession("user-1", "plan-1", TrainingSessionMode.TEXT, "idem-1");

        service.submitTaskAttempt(
                "user-1",
                started.sessionId(),
                "task-1",
                TaskAttemptInputType.TEXT,
                "Please do not store this sentence.",
                0,
                null,
                null,
                null,
                "attempt-idem-1");

        assertNull(trainingSessionRepository.lastSubmission.inputText());
        assertEquals(1, trainingSessionRepository.lastEvidence.size());
        assertEquals(false, trainingSessionRepository.lastEvidence.get(0).metadata().get("rawTextStored"));
    }

    private static final class FakeTrainingSessionRepository implements TrainingSessionRepository {

        private long sequence;
        private long attemptSequence;
        private final ConcurrentHashMap<String, TrainingSession> sessions = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> idempotency = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> attemptIdempotency = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<DailySummaryEvidence>> evidenceBySession = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, TrainingSessionCompletion> completions = new ConcurrentHashMap<>();
        private TaskAttemptSubmission lastSubmission;
        private List<LearningEvidenceDraft> lastEvidence = List.of();

        @Override
        public TrainingSession startDailySession(
                UserKey userKey,
                LearningPlan plan,
                TrainingSessionMode mode,
                String idempotencyKey
        ) {
            String idempotencyScope = userKey.value() + ":" + idempotencyKey;
            String existing = idempotency.get(idempotencyScope);
            if (existing != null) {
                return sessions.get(userKey.value() + ":" + existing);
            }
            TrainingSession session = TrainingSession.startDaily(
                    "training-" + ++sequence,
                    plan.planId(),
                    mode,
                    plan.tasks().get(0).taskId(),
                    Instant.parse("2026-08-10T08:00:00Z"));
            sessions.put(userKey.value() + ":" + session.sessionId(), session);
            idempotency.put(idempotencyScope, session.sessionId());
            return session;
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
            String key = userKey.value() + ":" + session.sessionId() + ":" + task.taskId() + ":" + idempotencyKey;
            lastSubmission = submission;
            lastEvidence = List.copyOf(evidence);
            String existing = attemptIdempotency.get(key);
            if (existing != null) {
                return TaskAttemptReceipt.accepted(existing, lastEvidence.size());
            }
            if (!session.currentTaskId().equals(task.taskId())) {
                throw new IllegalArgumentException("task is not the current training task");
            }
            String attemptId = "attempt-" + ++attemptSequence;
            attemptIdempotency.put(key, attemptId);
            if (nextTaskId != null) {
                sessions.put(userKey.value() + ":" + session.sessionId(), session.moveToTask(nextTaskId));
            }
            evidenceBySession.put(userKey.value() + ":" + session.sessionId(), toSummaryEvidence(lastEvidence));
            return TaskAttemptReceipt.accepted(attemptId, lastEvidence.size());
        }

        private List<DailySummaryEvidence> toSummaryEvidence(List<LearningEvidenceDraft> evidence) {
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

        private final Set<String> recordedPlanCompletions = ConcurrentHashMap.newKeySet();
        private int completionRecordCount;

        @Override
        public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LearningPlan getPlan(UserKey userKey, String planId) {
            if (!"user-1".equals(userKey.value()) || !"plan-1".equals(planId)) {
                throw new IllegalArgumentException("learning plan was not found");
            }
            return PLAN;
        }

        @Override
        public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
            if (!"user-1".equals(userKey.value()) || !"plan-1".equals(planId)) {
                throw new IllegalArgumentException("learning plan was not found");
            }
            if (practicedSkills.isEmpty() || evidenceCount <= 0) {
                throw new IllegalArgumentException("accepted learning evidence is required before planning can change");
            }
            if (recordedPlanCompletions.add(userKey.value() + ":" + planId)) {
                completionRecordCount++;
            }
        }
    }

    private static final class FakeUserProfileRepository implements UserProfileRepository {

        private OnboardingStep step = OnboardingStep.RESULT;
        private RawContentRetention rawTextRetention = RawContentRetention.STORE;

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
                    rawTextRetention,
                    RawContentRetention.STORE,
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
            return new OnboardingProgress(step, false, null);
        }
    }
}
