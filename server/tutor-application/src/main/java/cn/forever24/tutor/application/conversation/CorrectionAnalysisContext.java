package cn.forever24.tutor.application.conversation;

import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;

public record CorrectionAnalysisContext(
        TrainingSession session,
        LearningPlanTask currentTask,
        String message
) {

    public CorrectionAnalysisContext {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        if (currentTask == null) {
            throw new IllegalArgumentException("current task is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
