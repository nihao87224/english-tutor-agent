package cn.forever24.tutor.training;

public record AttemptCorrection(
        String sourceText,
        String suggestedText,
        String category,
        boolean critical,
        String explanation
) {
    public AttemptCorrection {
        sourceText = required(sourceText, "sourceText", 300);
        suggestedText = required(suggestedText, "suggestedText", 300);
        category = required(category, "category", 64);
        explanation = required(explanation, "explanation", 500);
    }

    private static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.strip().length() > maximum) {
            throw new IllegalArgumentException(field + " is required and exceeds its limit");
        }
        return value.strip();
    }
}
