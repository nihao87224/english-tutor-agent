package cn.forever24.tutor.application.conversation;

import java.util.List;
import java.util.Map;

public record LayeredCorrectionResult(
        boolean hasError,
        List<LayeredCorrectionItem> corrections,
        String overallFeedback,
        String promptVersion,
        String schemaVersion,
        String traceId,
        String providerId,
        String modelId
) {

    public LayeredCorrectionResult {
        corrections = List.copyOf(corrections == null ? List.of() : corrections);
        if (corrections.size() > 3) {
            throw new IllegalArgumentException("corrections must not exceed 3");
        }
        if (hasError && corrections.isEmpty()) {
            throw new IllegalArgumentException("error corrections are required when hasError is true");
        }
        if (!hasError && !corrections.isEmpty()) {
            throw new IllegalArgumentException("corrections must be empty when hasError is false");
        }
        overallFeedback = requireNonBlank(overallFeedback, "overall feedback");
        promptVersion = requireNonBlank(promptVersion, "prompt version");
        schemaVersion = requireNonBlank(schemaVersion, "schema version");
        traceId = requireNonBlank(traceId, "trace id");
        providerId = requireNonBlank(providerId, "provider id");
        modelId = requireNonBlank(modelId, "model id");
    }

    Map<String, Object> toEventData() {
        return Map.of(
                "hasError", hasError,
                "corrections", corrections.stream()
                        .map(LayeredCorrectionItem::toEventData)
                        .toList(),
                "overallFeedback", overallFeedback,
                "promptVersion", promptVersion,
                "schemaVersion", schemaVersion,
                "traceId", traceId,
                "providerId", providerId,
                "modelId", modelId);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
