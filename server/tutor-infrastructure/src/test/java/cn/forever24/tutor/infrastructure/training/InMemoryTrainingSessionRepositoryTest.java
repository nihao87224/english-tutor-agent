package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.learner.LearningEvidenceGenerator;
import cn.forever24.tutor.application.training.TrainingSessionCompletion;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTrainingSessionRepositoryTest {

    private final InMemoryTrainingSessionRepository repository = new InMemoryTrainingSessionRepository(
            Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void startIsIdempotentPerUserAndKey() {
        UserKey userKey = new UserKey("user-1");

        TrainingSession first = repository.startDailySession(userKey, plan(), TrainingSessionMode.TEXT, "idem-1");
        TrainingSession repeated = repository.startDailySession(userKey, plan(), TrainingSessionMode.MIXED, "idem-1");

        assertEquals(first.sessionId(), repeated.sessionId());
        assertEquals(TrainingSessionMode.TEXT, repeated.mode());
    }

    @Test
    void savesAndRestoresLifecycleStateForOwnerOnly() {
        UserKey userKey = new UserKey("user-1");
        TrainingSession started = repository.startDailySession(userKey, plan(), TrainingSessionMode.TEXT, "idem-1");
        repository.save(userKey, started.pause(Instant.parse("2026-08-10T08:01:00Z")));

        assertEquals(TrainingSessionStatus.PAUSED, repository.findById(userKey, started.sessionId()).orElseThrow().status());
        assertTrue(repository.findById(new UserKey("user-2"), started.sessionId()).isEmpty());
    }

    @Test
    void submitsTextAttemptIdempotentlyAndMovesToNextTask() {
        UserKey userKey = new UserKey("user-1");
        LearningPlan plan = plan();
        TrainingSession started = repository.startDailySession(userKey, plan, TrainingSessionMode.TEXT, "idem-1");
        TaskAttemptSubmission submission = TaskAttemptSubmission.text("A short text answer.", true, 1, 1200, null, null);

        TaskAttemptReceipt first = repository.submitTextAttempt(
                userKey,
                started,
                plan.tasks().get(0),
                submission,
                LearningEvidenceGenerator.fromTextAttempt(plan.tasks().get(0), submission, "A short text answer."),
                "attempt-idem-1",
                plan.tasks().get(1).taskId());
        TaskAttemptReceipt repeated = repository.submitTextAttempt(
                userKey,
                started,
                plan.tasks().get(0),
                submission,
                LearningEvidenceGenerator.fromTextAttempt(plan.tasks().get(0), submission, "A short text answer."),
                "attempt-idem-1",
                plan.tasks().get(1).taskId());

        assertEquals(first.attemptId(), repeated.attemptId());
        assertEquals(1, first.evidenceCount());
        assertEquals(first.evidenceCount(), repeated.evidenceCount());
        assertEquals(plan.tasks().get(1).taskId(), repository.findById(userKey, started.sessionId()).orElseThrow().currentTaskId());
        assertThrows(IllegalArgumentException.class, () -> repository.submitTextAttempt(
                userKey,
                started,
                plan.tasks().get(0),
                TaskAttemptSubmission.text("A different text answer.", true, 1, 1200, null, null),
                LearningEvidenceGenerator.fromTextAttempt(
                        plan.tasks().get(0),
                        TaskAttemptSubmission.text("A different text answer.", true, 1, 1200, null, null),
                        "A different text answer."),
                "attempt-idem-2",
                plan.tasks().get(1).taskId()));

        TrainingSession completedSession = repository.findById(userKey, started.sessionId())
                .orElseThrow()
                .complete(Instant.parse("2026-08-10T08:05:00Z"));
        TrainingSessionCompletion completion = repository.completeSession(userKey, completedSession);
        TrainingSessionCompletion repeatedCompletion = repository.completeSession(userKey, completedSession);

        assertEquals(TrainingSessionStatus.COMPLETED, completion.session().status());
        assertEquals(1, completion.dailySummary().evidenceCount());
        assertEquals(List.of("speaking"), completion.dailySummary().practicedSkills());
        assertEquals(completion.dailySummary(), repeatedCompletion.dailySummary());
    }

    private static LearningPlan plan() {
        return new LearningPlan(
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
    }
}
