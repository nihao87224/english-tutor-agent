package cn.forever24.tutor.reporting;

import java.time.Instant;
import java.util.List;

public record DailyTrainingSummary(
        String sessionId,
        int completedTaskCount,
        int evidenceCount,
        List<String> practicedSkills,
        List<String> highlights,
        List<String> memorableItems,
        List<String> nextFocus,
        Instant generatedAt
) {

    public DailyTrainingSummary {
        sessionId = requireNonBlank(sessionId, "session id");
        if (completedTaskCount < 0) {
            throw new IllegalArgumentException("completedTaskCount must not be negative");
        }
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount must not be negative");
        }
        practicedSkills = copyLimited(practicedSkills, "practiced skills", 6);
        highlights = copyLimited(highlights, "highlights", 5);
        memorableItems = copyLimited(memorableItems, "memorable items", 5);
        nextFocus = copyLimited(nextFocus, "next focus", 5);
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt is required");
        }
    }

    private static List<String> copyLimited(List<String> values, String fieldName, int maxSize) {
        List<String> safeValues = List.copyOf(values == null ? List.of() : values);
        if (safeValues.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " are required");
        }
        if (safeValues.size() > maxSize) {
            throw new IllegalArgumentException(fieldName + " must contain at most " + maxSize + " items");
        }
        safeValues.forEach(value -> requireNonBlank(value, fieldName + " item"));
        return safeValues;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
