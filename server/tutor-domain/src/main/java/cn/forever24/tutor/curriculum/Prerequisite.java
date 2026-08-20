package cn.forever24.tutor.curriculum;

import java.math.BigDecimal;

public record Prerequisite(String skillKey, BigDecimal minimumMastery, BigDecimal minimumConfidence) {

    public Prerequisite {
        if (skillKey == null || skillKey.isBlank()) {
            throw new IllegalArgumentException("prerequisite skillKey is required");
        }
        skillKey = skillKey.strip();
        requireProbability(minimumMastery, "minimumMastery");
        requireProbability(minimumConfidence, "minimumConfidence");
    }

    private static void requireProbability(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
