package cn.forever24.tutor.assessment;

import java.math.BigDecimal;

public record OpenAnswerEvaluation(
        AssessmentCorrectness correctness,
        BigDecimal score,
        BigDecimal evaluatorConfidence,
        String feedback,
        String promptVersion,
        String schemaVersion
) {

    public OpenAnswerEvaluation {
        if (correctness == null) {
            throw new IllegalArgumentException("correctness is required");
        }
        if (score == null
                || score.compareTo(ObjectiveAnswerScore.ZERO_SCORE) < 0
                || score.compareTo(ObjectiveAnswerScore.FULL_SCORE) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
        if (evaluatorConfidence == null
                || evaluatorConfidence.compareTo(ObjectiveAnswerScore.ZERO_SCORE) < 0
                || evaluatorConfidence.compareTo(ObjectiveAnswerScore.FULL_SCORE) > 0) {
            throw new IllegalArgumentException("evaluator confidence must be between 0 and 1");
        }
        if (feedback == null || feedback.isBlank() || feedback.length() > 500) {
            throw new IllegalArgumentException("feedback must be 1-500 characters");
        }
        if (promptVersion == null || promptVersion.isBlank()) {
            throw new IllegalArgumentException("prompt version is required");
        }
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schema version is required");
        }
    }

    public static OpenAnswerEvaluation safeUnscored(String promptVersion, String schemaVersion) {
        return new OpenAnswerEvaluation(
                AssessmentCorrectness.UNSCORED,
                ObjectiveAnswerScore.ZERO_SCORE,
                ObjectiveAnswerScore.ZERO_SCORE,
                "Evaluation unavailable; answer saved for later review.",
                promptVersion,
                schemaVersion);
    }
}
