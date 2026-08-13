package cn.forever24.tutor.application.conversation;

import java.util.List;
import java.util.Map;

public record LayeredCorrectionItem(
        String original,
        String corrected,
        String errorType,
        CorrectionSeverity severity,
        String explanationZh,
        boolean shouldInterrupt,
        boolean memoryWorthy,
        List<CorrectionSuggestion> naturalSuggestions
) {

    public LayeredCorrectionItem {
        original = requireNonBlank(original, "original");
        corrected = requireNonBlank(corrected, "corrected");
        errorType = requireNonBlank(errorType, "error type");
        if (severity == null) {
            throw new IllegalArgumentException("severity is required");
        }
        explanationZh = explanationZh == null ? "" : explanationZh;
        naturalSuggestions = List.copyOf(naturalSuggestions == null ? List.of() : naturalSuggestions);
        if (naturalSuggestions.size() > 3) {
            throw new IllegalArgumentException("natural suggestions must not exceed 3");
        }
        if (severity == CorrectionSeverity.LOW && shouldInterrupt) {
            throw new IllegalArgumentException("low severity corrections must not interrupt");
        }
    }

    Map<String, Object> toEventData() {
        return Map.of(
                "original", original,
                "corrected", corrected,
                "errorType", errorType,
                "severity", severity.name(),
                "explanationZh", explanationZh,
                "shouldInterrupt", shouldInterrupt,
                "memoryWorthy", memoryWorthy,
                "naturalSuggestions", naturalSuggestions.stream()
                        .map(CorrectionSuggestion::toEventData)
                        .toList());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
