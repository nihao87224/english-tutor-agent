package cn.forever24.tutor.training;

import java.time.Instant;

public record LessonAttempt(
        String attemptId,
        String sessionId,
        String taskId,
        TaskAttemptInputType inputType,
        String text,
        LessonAttemptStatus status,
        LessonObjectiveResult objectiveResult,
        Instant submittedAt,
        long version
) {
    public LessonAttempt {
        requireText(attemptId, "attemptId");
        requireText(sessionId, "sessionId");
        requireText(taskId, "taskId");
        if (inputType == null || status == null || submittedAt == null) {
            throw new IllegalArgumentException("attempt type, status and submittedAt are required");
        }
        if (inputType == TaskAttemptInputType.TEXT && (text == null || text.isBlank())) {
            throw new IllegalArgumentException("text is required for a TEXT attempt");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
