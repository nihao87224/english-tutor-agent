package cn.forever24.tutor.assessment;

import java.math.BigDecimal;

public record AssessmentAttemptEvidence(
        String itemId,
        AssessmentCorrectness correctness,
        BigDecimal score,
        BigDecimal evaluatorConfidence
) {

    public AssessmentAttemptEvidence {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
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
    }
}
