package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.learner.LearningEvidenceGenerator;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.CurrentTrainingTask;
import cn.forever24.tutor.training.TaskAttemptInputType;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import cn.forever24.tutor.training.TrainingSessionStatus;
import cn.forever24.tutor.training.TrainingTaskStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class TrainingSessionApplicationService {

    private final UserProfileRepository userProfileRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final Clock clock;

    public TrainingSessionApplicationService(
            UserProfileRepository userProfileRepository,
            LearningPlanRepository learningPlanRepository,
            TrainingSessionRepository trainingSessionRepository,
            Clock clock
    ) {
        this.userProfileRepository = userProfileRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.clock = clock;
    }

    public TrainingSession startDailySession(
            String userKeyValue,
            String planId,
            TrainingSessionMode mode,
            String idempotencyKey
    ) {
        UserKey userKey = readyUser(userKeyValue);
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        LearningPlan plan = learningPlanRepository.getPlan(userKey, planId);
        return trainingSessionRepository.startDailySession(userKey, plan, mode, idempotencyKey);
    }

    public TrainingSession getSession(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        return trainingSessionRepository.findById(userKey, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("training session was not found"));
    }

    public TrainingSession pause(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        TrainingSession session = getSession(userKey.value(), sessionId);
        return trainingSessionRepository.save(userKey, session.pause(clock.instant()));
    }

    public TrainingSession resume(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        TrainingSession session = getSession(userKey.value(), sessionId);
        return trainingSessionRepository.save(userKey, session.resume());
    }

    public TrainingSessionCompletion complete(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        TrainingSession session = getSession(userKey.value(), sessionId);
        TrainingSessionCompletion completion;
        if (session.status() == TrainingSessionStatus.COMPLETED) {
            completion = trainingSessionRepository.completeSession(userKey, session);
        } else {
            completion = trainingSessionRepository.completeSession(userKey, session.complete(clock.instant()));
        }
        learningPlanRepository.recordTrainingCompletion(
                userKey,
                completion.session().planId(),
                completion.dailySummary().practicedSkills(),
                completion.dailySummary().evidenceCount());
        return completion;
    }

    public CurrentTrainingTask getCurrentTask(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        TrainingSession session = getSession(userKey.value(), sessionId);
        if (session.status().terminal()) {
            throw new IllegalArgumentException("training session has no current task");
        }
        LearningPlan plan = learningPlanRepository.getPlan(userKey, session.planId());
        LearningPlanTask task = plan.tasks().stream()
                .filter(candidate -> candidate.taskId().equals(session.currentTaskId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("current training task was not found"));
        return new CurrentTrainingTask(task, TrainingTaskStatus.STARTED);
    }

    public TaskAttemptReceipt submitTaskAttempt(
            String userKeyValue,
            String sessionId,
            String taskId,
            TaskAttemptInputType inputType,
            String text,
            Integer hintLevel,
            Integer clientDurationMs,
            Instant clientStartedAt,
            Instant clientCompletedAt,
            String idempotencyKey
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        if (inputType == null) {
            throw new IllegalArgumentException("inputType is required");
        }
        if (inputType != TaskAttemptInputType.TEXT) {
            throw new IllegalArgumentException("only TEXT task attempts are supported in M2-T02");
        }
        TrainingSession session = getSession(userKey.value(), sessionId);
        if (session.status() != TrainingSessionStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("training session must be IN_PROGRESS to submit an attempt");
        }
        LearningPlan plan = learningPlanRepository.getPlan(userKey, session.planId());
        List<LearningPlanTask> tasks = plan.tasks();
        LearningPlanTask task = tasks.stream()
                .filter(candidate -> candidate.taskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("training task was not found"));
        PrivacySettings privacySettings = userProfileRepository.getPrivacySettings(userKey);
        TaskAttemptSubmission submission = TaskAttemptSubmission.text(
                text,
                privacySettings.saveRawText(),
                hintLevel,
                clientDurationMs,
                clientStartedAt,
                clientCompletedAt);
        List<LearningEvidenceDraft> evidence = LearningEvidenceGenerator.fromTextAttempt(task, submission, text);
        return trainingSessionRepository.submitTextAttempt(
                userKey,
                session,
                task,
                submission,
                evidence,
                idempotencyKey,
                session.currentTaskId().equals(taskId) ? nextTaskId(tasks, taskId) : null);
    }

    private UserKey readyUser(String userKeyValue) {
        UserKey userKey = new UserKey(userKeyValue);
        OnboardingProgress progress = userProfileRepository.getOnboardingProgress(userKey);
        if (progress.step().ordinal() < OnboardingStep.RESULT.ordinal()) {
            throw new IllegalArgumentException("initial assessment result is required before training");
        }
        return userKey;
    }

    private String nextTaskId(List<LearningPlanTask> tasks, String currentTaskId) {
        for (int index = 0; index < tasks.size(); index++) {
            if (tasks.get(index).taskId().equals(currentTaskId) && index + 1 < tasks.size()) {
                return tasks.get(index + 1).taskId();
            }
        }
        return null;
    }
}
