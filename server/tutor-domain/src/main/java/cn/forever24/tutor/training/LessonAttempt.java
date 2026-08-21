package cn.forever24.tutor.training;

import java.time.Instant;

public record LessonAttempt(
        String attemptId,
        String sessionId,
        String taskId,
        TaskAttemptInputType inputType,
        String text,
        String audioAssetId,
        String transcript,
        Double asrConfidence,
        boolean transcriptConfirmed,
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
        if (inputType == TaskAttemptInputType.AUDIO && (audioAssetId == null || audioAssetId.isBlank())) {
            throw new IllegalArgumentException("audioAssetId is required for an AUDIO attempt");
        }
        if (asrConfidence != null && (asrConfidence < 0 || asrConfidence > 1)) {
            throw new IllegalArgumentException("asrConfidence must be between 0 and 1");
        }
        if (transcriptConfirmed && (transcript == null || transcript.isBlank())) {
            throw new IllegalArgumentException("a confirmed transcript is required");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }

    public LessonAttempt withTranscription(
            String value,
            Double confidence,
            boolean confirmed,
            LessonAttemptStatus nextStatus
    ) {
        return new LessonAttempt(
                attemptId, sessionId, taskId, inputType, text, audioAssetId, value, confidence,
                confirmed, nextStatus, objectiveResult, submittedAt, version + 1);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
