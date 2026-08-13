package cn.forever24.tutor.profile;

public enum CorrectionStyle {
    LIGHT,
    STANDARD,
    STRICT;

    public static CorrectionStyle fromContractValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("correction style is required");
        }
        try {
            return CorrectionStyle.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported correction style: " + value, exception);
        }
    }
}
