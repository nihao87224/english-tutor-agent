package cn.forever24.tutor.ai.provider;

public final class ProviderText {

    private ProviderText() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, fieldName + " must not be blank");
        }
        return value;
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, fieldName + " must not be null");
        }
        return value;
    }
}
