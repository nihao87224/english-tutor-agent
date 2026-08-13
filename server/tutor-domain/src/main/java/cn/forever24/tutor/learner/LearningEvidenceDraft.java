package cn.forever24.tutor.learner;

import java.math.BigDecimal;
import java.util.Map;

public record LearningEvidenceDraft(
        String skillDimension,
        String knowledgeKey,
        EvidenceType evidenceType,
        EvidenceResult result,
        BigDecimal rawScore,
        BigDecimal weight,
        BigDecimal independence,
        BigDecimal transferLevel,
        int delayDays,
        BigDecimal evaluatorConfidence,
        Map<String, Object> metadata
) {

    public LearningEvidenceDraft {
        skillDimension = requireNonBlank(skillDimension, "skill dimension").toLowerCase();
        knowledgeKey = requireNonBlank(knowledgeKey, "knowledge key");
        if (knowledgeKey.length() > 150) {
            throw new IllegalArgumentException("knowledge key must be at most 150 characters");
        }
        if (evidenceType == null) {
            throw new IllegalArgumentException("evidence type is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("evidence result is required");
        }
        rawScore = requireRatio(rawScore, "raw score");
        weight = requireRatio(weight, "weight");
        independence = requireRatio(independence, "independence");
        transferLevel = requireRatio(transferLevel, "transfer level");
        if (delayDays < 0) {
            throw new IllegalArgumentException("delay days must not be negative");
        }
        evaluatorConfidence = requireRatio(evaluatorConfidence, "evaluator confidence");
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private static BigDecimal requireRatio(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1");
        }
        return value;
    }
}
