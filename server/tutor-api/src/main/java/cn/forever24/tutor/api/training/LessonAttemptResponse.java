package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.LessonAttemptMutationResult;

import java.time.Instant;

public record LessonAttemptResponse(
        String attemptId,
        String taskId,
        String inputType,
        String status,
        Instant submittedAt,
        Integer pollAfterMs,
        Transcript transcript,
        ObjectiveResult objectiveResult,
        LessonSessionResponse.AttemptProgress stepProgress,
        long version
) {
    static LessonAttemptResponse from(LessonAttemptMutationResult result) {
        var attempt = result.attempt();
        return new LessonAttemptResponse(
                attempt.attemptId(), attempt.taskId(), attempt.inputType().name(), attempt.status().name(),
                attempt.submittedAt(), attempt.status().name().endsWith("PENDING") ? 1000 : null,
                attempt.transcript() == null ? null : new Transcript(
                        attempt.transcript(), attempt.asrConfidence(), !attempt.transcriptConfirmed()),
                attempt.objectiveResult() == null ? null : new ObjectiveResult(
                        attempt.objectiveResult().correct(), attempt.objectiveResult().expectedAnswer(),
                        attempt.objectiveResult().explanation()),
                LessonSessionResponse.AttemptProgress.from(result.progress()), attempt.version());
    }

    public record ObjectiveResult(boolean correct, String expectedAnswer, String explanation) {
    }

    public record Transcript(String text, Double confidence, boolean confirmationRequired) { }
}
