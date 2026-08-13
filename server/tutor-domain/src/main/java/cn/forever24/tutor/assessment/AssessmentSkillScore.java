package cn.forever24.tutor.assessment;

import java.math.BigDecimal;
import java.util.List;

public record AssessmentSkillScore(
        BigDecimal score,
        String level,
        BigDecimal confidence,
        List<String> evidence
) {

    public AssessmentSkillScore {
        if (score == null
                || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(new BigDecimal("100.0000")) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("level is required");
        }
        if (confidence == null
                || confidence.compareTo(ObjectiveAnswerScore.ZERO_SCORE) < 0
                || confidence.compareTo(ObjectiveAnswerScore.FULL_SCORE) > 0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        if (evidence.size() > 5) {
            throw new IllegalArgumentException("evidence must contain at most 5 items");
        }
    }
}
