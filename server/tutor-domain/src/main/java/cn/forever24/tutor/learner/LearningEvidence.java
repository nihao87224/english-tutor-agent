package cn.forever24.tutor.learner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record LearningEvidence(
        String evidenceId,
        String attemptId,
        String source,
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
        Map<String, Object> metadata,
        Instant occurredAt
) {

    public LearningEvidence {
        evidenceId = requireNonBlank(evidenceId, "evidence id");
        attemptId = requireNonBlank(attemptId, "attempt id");
        source = requireNonBlank(source, "source");
        skillDimension = requireNonBlank(skillDimension, "skill dimension").toLowerCase();
        knowledgeKey = requireNonBlank(knowledgeKey, "knowledge key");
        if (evidenceType == null) {
            throw new IllegalArgumentException("evidence type is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("evidence result is required");
        }
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurred at is required");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
