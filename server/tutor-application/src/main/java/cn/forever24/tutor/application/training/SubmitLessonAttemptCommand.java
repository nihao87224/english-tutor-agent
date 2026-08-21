package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.TaskAttemptInputType;

import java.time.Instant;

public record SubmitLessonAttemptCommand(
        String taskId,
        TaskAttemptInputType inputType,
        String text,
        String retryOfAttemptId,
        Instant clientStartedAt,
        Integer clientDurationMs
) {
    public SubmitLessonAttemptCommand {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("taskId is required");
        if (inputType == null) throw new IllegalArgumentException("inputType is required");
        taskId = taskId.strip();
        text = text == null ? null : text.strip();
        if (inputType == TaskAttemptInputType.TEXT && (text == null || text.isBlank())) {
            throw new IllegalArgumentException("text is required for a TEXT attempt");
        }
        if (text != null && text.length() > 4000) throw new IllegalArgumentException("text must not exceed 4000 characters");
        if (clientDurationMs != null && clientDurationMs < 0) throw new IllegalArgumentException("clientDurationMs must not be negative");
    }
}
