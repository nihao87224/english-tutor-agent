package cn.forever24.tutor.training;

import java.time.Instant;

public record LessonAttempt(
        String attemptId,
        String sessionId,
        String taskId,
        String retryOfAttemptId,
        TaskAttemptInputType inputType,
        String text,
        String audioAssetId,
        String transcript,
        Double asrConfidence,
        boolean transcriptConfirmed,
        LessonAttemptStatus status,
        LessonObjectiveResult objectiveResult,
        AttemptAnalysis analysis,
        String analysisErrorCode,
        Instant submittedAt,
        long version
) {
    public LessonAttempt {
        requireText(attemptId, "attemptId");
        requireText(sessionId, "sessionId");
        requireText(taskId, "taskId");
        if (retryOfAttemptId != null && retryOfAttemptId.isBlank()) {
            throw new IllegalArgumentException("retryOfAttemptId must not be blank");
        }
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
        if (analysisErrorCode != null && (analysisErrorCode.isBlank() || analysisErrorCode.length() > 64)) {
            throw new IllegalArgumentException("analysisErrorCode must be nonblank and at most 64 characters");
        }
    }

    public LessonAttempt(
            String attemptId, String sessionId, String taskId, TaskAttemptInputType inputType, String text,
            String audioAssetId, String transcript, Double asrConfidence, boolean transcriptConfirmed,
            LessonAttemptStatus status, LessonObjectiveResult objectiveResult, Instant submittedAt, long version
    ) {
        this(attemptId, sessionId, taskId, null, inputType, text, audioAssetId, transcript, asrConfidence,
                transcriptConfirmed, status, objectiveResult, null, null, submittedAt, version);
    }

    public LessonAttempt withTranscription(
            String value,
            Double confidence,
            boolean confirmed,
            LessonAttemptStatus nextStatus
    ) {
        return new LessonAttempt(
                attemptId, sessionId, taskId, retryOfAttemptId, inputType, text, audioAssetId, value, confidence,
                confirmed, nextStatus, objectiveResult, analysis, analysisErrorCode, submittedAt, version + 1);
    }

    public LessonAttempt withAnalysis(AttemptAnalysis value, LessonAttemptStatus nextStatus) {
        if (value == null || nextStatus == null) throw new IllegalArgumentException("analysis and status are required");
        return new LessonAttempt(attemptId, sessionId, taskId, retryOfAttemptId, inputType, text, audioAssetId, transcript,
                asrConfidence, transcriptConfirmed, nextStatus, objectiveResult, value, null, submittedAt, version + 1);
    }

    public LessonAttempt withAnalysisFailure(String errorCode, boolean retryable) {
        return new LessonAttempt(attemptId, sessionId, taskId, retryOfAttemptId, inputType, text, audioAssetId, transcript,
                asrConfidence, transcriptConfirmed,
                retryable ? LessonAttemptStatus.ANALYSIS_RETRYABLE : LessonAttemptStatus.ANALYSIS_FAILED,
                objectiveResult, null, errorCode, submittedAt, version + 1);
    }

    public LessonAttempt withEvidenceRecorded() {
        return new LessonAttempt(attemptId, sessionId, taskId, retryOfAttemptId, inputType, text, audioAssetId, transcript,
                asrConfidence, transcriptConfirmed, LessonAttemptStatus.EVIDENCE_RECORDED,
                objectiveResult, analysis, null, submittedAt, version + 1);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
