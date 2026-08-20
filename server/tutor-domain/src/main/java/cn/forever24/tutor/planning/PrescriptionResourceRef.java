package cn.forever24.tutor.planning;

public record PrescriptionResourceRef(String resourceKey, String resourceVersion) {

    public PrescriptionResourceRef {
        resourceKey = required(resourceKey, "resourceKey", 180);
        resourceVersion = required(resourceVersion, "resourceVersion", 32);
    }

    private static String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.strip().length() > maximumLength) {
            throw new IllegalArgumentException("valid " + field + " is required");
        }
        return value.strip();
    }
}
