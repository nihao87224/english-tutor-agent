package cn.forever24.tutor.training;

import java.util.List;

/** Validated feedback only. Raw provider payloads are deliberately not retained here. */
public record AttemptAnalysis(
        String summary,
        List<AttemptCriterionResult> criteria,
        List<AttemptCorrection> corrections,
        List<String> naturalExpressions,
        String promptVersion,
        String providerId,
        String modelId,
        String traceId
) {
    public AttemptAnalysis {
        if (summary == null || summary.isBlank() || summary.strip().length() > 1000) {
            throw new IllegalArgumentException("analysis summary is required and must be at most 1000 characters");
        }
        summary = summary.strip();
        criteria = List.copyOf(criteria == null ? List.of() : criteria);
        corrections = List.copyOf(corrections == null ? List.of() : corrections);
        naturalExpressions = List.copyOf(naturalExpressions == null ? List.of() : naturalExpressions.stream()
                .map(value -> value == null ? "" : value.strip()).filter(value -> !value.isBlank()).toList());
        if (criteria.isEmpty() || criteria.stream().anyMatch(java.util.Objects::isNull)
                || corrections.stream().anyMatch(java.util.Objects::isNull)
                || naturalExpressions.size() > 3 || corrections.size() > 3) {
            throw new IllegalArgumentException("analysis criteria and feedback limits are invalid");
        }
        if (criteria.stream().map(AttemptCriterionResult::criterionKey).distinct().count() != criteria.size()) {
            throw new IllegalArgumentException("analysis criteria must be unique");
        }
        promptVersion = required(promptVersion, "promptVersion");
        providerId = required(providerId, "providerId");
        modelId = required(modelId, "modelId");
        traceId = required(traceId, "traceId");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || value.strip().length() > 128) {
            throw new IllegalArgumentException(field + " is required and exceeds its limit");
        }
        return value.strip();
    }
}
