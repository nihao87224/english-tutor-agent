package cn.forever24.tutor.planning;

public record PrescriptionGoal(String code, String label) {

    public PrescriptionGoal {
        code = required(code, "code", 64);
        label = required(label, "label", 160);
    }

    private static String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.strip().length() > maximumLength) {
            throw new IllegalArgumentException("valid prescription goal " + field + " is required");
        }
        return value.strip();
    }
}
