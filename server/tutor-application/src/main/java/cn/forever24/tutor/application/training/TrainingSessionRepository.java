package cn.forever24.tutor.application.training;

import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;

import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository {

    TrainingSession startDailySession(
            UserKey userKey,
            LearningPlan plan,
            TrainingSessionMode mode,
            String idempotencyKey
    );

    Optional<TrainingSession> findById(UserKey userKey, String sessionId);

    TrainingSession save(UserKey userKey, TrainingSession session);

    TrainingSessionCompletion completeSession(UserKey userKey, TrainingSession session);

    TaskAttemptReceipt submitTextAttempt(
            UserKey userKey,
            TrainingSession session,
            LearningPlanTask task,
            TaskAttemptSubmission submission,
            List<LearningEvidenceDraft> evidence,
            String idempotencyKey,
            String nextTaskId
    );
}
