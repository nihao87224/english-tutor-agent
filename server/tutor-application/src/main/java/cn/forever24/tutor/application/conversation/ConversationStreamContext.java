package cn.forever24.tutor.application.conversation;

import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;

public record ConversationStreamContext(
        TrainingSession session,
        LearningPlanTask currentTask,
        String message
) {

    public ConversationStreamContext {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        if (currentTask == null) {
            throw new IllegalArgumentException("currentTask is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
