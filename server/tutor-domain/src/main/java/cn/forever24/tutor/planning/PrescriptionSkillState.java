package cn.forever24.tutor.planning;

import cn.forever24.tutor.curriculum.CefrLevel;

import java.math.BigDecimal;
import java.time.Instant;

public record PrescriptionSkillState(
        String skillKey,
        BigDecimal mastery,
        BigDecimal confidence,
        CefrLevel level,
        int evidenceCount,
        Instant lastEvidenceAt
) {

    public PrescriptionSkillState {
        if (skillKey == null || skillKey.isBlank()) {
            throw new IllegalArgumentException("skillKey is required");
        }
        skillKey = skillKey.strip();
        mastery = probability(mastery, "mastery");
        confidence = probability(confidence, "confidence");
        if (level == null || lastEvidenceAt == null || evidenceCount < 0) {
            throw new IllegalArgumentException("valid skill state metadata is required");
        }
    }

    private static BigDecimal probability(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
        return value;
    }
}
