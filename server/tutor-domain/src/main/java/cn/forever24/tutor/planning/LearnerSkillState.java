package cn.forever24.tutor.planning;

import java.math.BigDecimal;

public record LearnerSkillState(
        String dimension,
        BigDecimal estimate,
        BigDecimal confidence,
        String level,
        int evidenceCount
) {

    public LearnerSkillState {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension is required");
        }
        if (estimate == null || estimate.compareTo(BigDecimal.ZERO) < 0 || estimate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("estimate must be between 0 and 1");
        }
        if (confidence == null
                || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("level is required");
        }
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount must not be negative");
        }
    }
}
