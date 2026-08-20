package cn.forever24.tutor.resource;

import java.util.regex.Pattern;

final class ResourceValidation {

    private static final Pattern SEMANTIC_VERSION = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$");
    private static final Pattern CONTENT_HASH = Pattern.compile("^sha256:[a-f0-9]{64}$");

    private ResourceValidation() {
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    static String semanticVersion(String value) {
        required(value, "semanticVersion");
        if (!SEMANTIC_VERSION.matcher(value).matches()) {
            throw new IllegalArgumentException("semanticVersion must use MAJOR.MINOR.PATCH");
        }
        return value;
    }

    static String contentHash(String value) {
        required(value, "contentHash");
        if (!CONTENT_HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("contentHash must be a sha256 digest");
        }
        return value;
    }
}
