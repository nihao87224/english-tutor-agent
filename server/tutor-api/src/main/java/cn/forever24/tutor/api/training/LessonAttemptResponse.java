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
        Analysis analysis,
        String analysisErrorCode,
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
                attempt.analysis() == null ? null : new Analysis(attempt.analysis().summary(),
                        attempt.analysis().criteria().stream().map(value -> new Criterion(
                                value.criterionKey(), value.satisfied(), value.feedback())).toList(),
                        attempt.analysis().corrections().stream().map(value -> new Correction(value.sourceText(),
                                value.suggestedText(), value.category(), value.critical(), value.explanation())).toList(),
                        attempt.analysis().naturalExpressions()),
                attempt.analysisErrorCode(),
                LessonSessionResponse.AttemptProgress.from(result.progress()), attempt.version());
    }

    public record ObjectiveResult(boolean correct, String expectedAnswer, String explanation) {
    }

    public record Transcript(String text, Double confidence, boolean confirmationRequired) { }
    public record Analysis(String summary, java.util.List<Criterion> criteria,
                           java.util.List<Correction> corrections, java.util.List<String> naturalExpressions) { }
    public record Criterion(String criterionKey, boolean satisfied, String feedback) { }
    public record Correction(String sourceText, String suggestedText, String category, boolean critical, String explanation) { }
}
