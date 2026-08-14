package cn.forever24.tutor.application.provider;

public class AiProviderConfigurationException extends RuntimeException {

    private final String code;
    private final int status;

    private AiProviderConfigurationException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AiProviderConfigurationException notFound(String providerCode) {
        return new AiProviderConfigurationException("AI_PROVIDER_NOT_FOUND", "AI provider was not found: " + providerCode, 404);
    }

    public static AiProviderConfigurationException invalid(String message) {
        return new AiProviderConfigurationException("INVALID_AI_PROVIDER_CONFIGURATION", message, 400);
    }

    public static AiProviderConfigurationException unavailable(String message) {
        return new AiProviderConfigurationException("AI_PROVIDER_CONFIGURATION_UNAVAILABLE", message, 503);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
