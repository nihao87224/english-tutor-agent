package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;

import java.net.URI;
import java.time.Duration;

public final class OpenAiProviderProperties {

    public static final String PROVIDER_ID = "openai";

    private final URI baseUrl;
    private final String apiKey;
    private final String llmModel;
    private final String asrModel;
    private final String ttsModel;
    private final String ttsVoice;
    private final Duration timeout;

    private OpenAiProviderProperties(
            URI baseUrl,
            String apiKey,
            String llmModel,
            String asrModel,
            String ttsModel,
            String ttsVoice,
            Duration timeout
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = requireNonBlank(apiKey, "apiKey");
        this.llmModel = normalizeOptional(llmModel);
        this.asrModel = normalizeOptional(asrModel);
        this.ttsModel = normalizeOptional(ttsModel);
        this.ttsVoice = normalizeOptional(ttsVoice);
        this.timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
    }

    public static OpenAiProviderProperties fromActiveConfiguration(ActiveAiProviderConfiguration configuration) {
        return new OpenAiProviderProperties(
                configuration.baseUrl(),
                configuration.apiKey(),
                configuration.llmModel(),
                configuration.asrModel(),
                configuration.ttsModel(),
                configuration.ttsVoice(),
                configuration.timeout());
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String llmModel() {
        return requireNonBlank(llmModel, "llmModel");
    }

    public String asrModel() {
        return requireNonBlank(asrModel, "asrModel");
    }

    public String ttsModel() {
        return requireNonBlank(ttsModel, "ttsModel");
    }

    public String ttsVoice() {
        return requireNonBlank(ttsVoice, "ttsVoice");
    }

    public Duration timeout() {
        return timeout;
    }

    private static String requireNonBlank(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " must be configured for the OpenAI provider");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
