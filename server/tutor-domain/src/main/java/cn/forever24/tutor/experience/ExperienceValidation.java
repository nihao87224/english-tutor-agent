package cn.forever24.tutor.experience;

import java.util.Set;
import java.util.regex.Pattern;

final class ExperienceValidation {

    private static final Pattern EXTERNAL_KEY = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,199}$");
    private static final Pattern SEMANTIC_VERSION = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?$");

    private ExperienceValidation() {
    }

    static String requiredText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " exceeds " + maximumLength + " characters");
        }
        return normalized;
    }

    static String externalKey(String value, String field, int maximumLength) {
        String normalized = requiredText(value, field, maximumLength);
        if (!EXTERNAL_KEY.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " has an invalid format");
        }
        return normalized;
    }

    static String semanticVersion(String value) {
        String normalized = requiredText(value, "resourceVersion", 32);
        if (!SEMANTIC_VERSION.matcher(normalized).matches()) {
            throw new IllegalArgumentException("resourceVersion must be semantic version text");
        }
        return normalized;
    }

    static Set<String> tags(Set<String> values, String field, boolean required) {
        if (values == null || values.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(field + " must not be empty");
            }
            return Set.of();
        }
        java.util.LinkedHashSet<String> normalized = new java.util.LinkedHashSet<>();
        for (String value : values) {
            normalized.add(requiredText(value, field, 120));
        }
        return Set.copyOf(normalized);
    }
}
