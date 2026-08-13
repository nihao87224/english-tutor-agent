package cn.forever24.tutor.assessment;

import java.math.BigDecimal;

public record ObjectiveAnswerScore(
        AssessmentCorrectness correctness,
        BigDecimal score,
        BigDecimal evaluatorConfidence
) {

    public static final BigDecimal FULL_SCORE = new BigDecimal("1.0000");
    public static final BigDecimal ZERO_SCORE = new BigDecimal("0.0000");
    public static final BigDecimal DETERMINISTIC_CONFIDENCE = new BigDecimal("1.0000");

    public ObjectiveAnswerScore {
        if (correctness == null) {
            throw new IllegalArgumentException("correctness is required");
        }
        if (score == null || score.compareTo(ZERO_SCORE) < 0 || score.compareTo(FULL_SCORE) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
        if (evaluatorConfidence == null
                || evaluatorConfidence.compareTo(ZERO_SCORE) < 0
                || evaluatorConfidence.compareTo(FULL_SCORE) > 0) {
            throw new IllegalArgumentException("evaluator confidence must be between 0 and 1");
        }
    }
}
