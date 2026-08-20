package cn.forever24.tutor.curriculum;

import java.math.BigDecimal;

public record EvidenceCriterion(
        String criterionKey,
        String description,
        BigDecimal weight,
        boolean required,
        int sequence
) {

    public EvidenceCriterion {
        if (criterionKey == null || criterionKey.isBlank()) {
            throw new IllegalArgumentException("criterionKey is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("evidence criterion description is required");
        }
        criterionKey = criterionKey.strip();
        description = description.strip();
        if (criterionKey.length() > 128 || description.length() > 500) {
            throw new IllegalArgumentException("evidence criterion exceeds storage limits");
        }
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("evidence criterion weight must be greater than 0 and at most 1");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("evidence criterion sequence must not be negative");
        }
    }
}
