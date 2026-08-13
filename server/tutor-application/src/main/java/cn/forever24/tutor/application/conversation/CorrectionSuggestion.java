package cn.forever24.tutor.application.conversation;

import java.util.Map;

public record CorrectionSuggestion(
        String sentence,
        CorrectionSuggestionStyle style
) {

    public CorrectionSuggestion {
        sentence = requireNonBlank(sentence, "suggestion sentence");
        if (style == null) {
            throw new IllegalArgumentException("suggestion style is required");
        }
    }

    Map<String, Object> toEventData() {
        return Map.of(
                "sentence", sentence,
                "style", style.name());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
