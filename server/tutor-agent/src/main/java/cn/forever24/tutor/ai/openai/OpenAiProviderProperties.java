package cn.forever24.tutor.ai.openai;

import org.springframework.core.env.Environment;

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
        this.apiKey = requireNonBlank(apiKey, "OPENAI_API_KEY");
        this.llmModel = requireNonBlank(llmModel, "OPENAI_LLM_MODEL");
        this.asrModel = requireNonBlank(asrModel, "OPENAI_ASR_MODEL");
        this.ttsModel = requireNonBlank(ttsModel, "OPENAI_TTS_MODEL");
        this.ttsVoice = requireNonBlank(ttsVoice, "OPENAI_TTS_VOICE");
        this.timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
    }

    public static OpenAiProviderProperties fromEnvironment(Environment environment) {
        String baseUrl = firstNonBlank(environment, "OPENAI_BASE_URL", "LLM_BASE_URL");
        return new OpenAiProviderProperties(
                URI.create(stripTrailingSlash(baseUrl == null ? "https://api.openai.com/v1" : baseUrl)),
                firstNonBlank(environment, "OPENAI_API_KEY", "LLM_API_KEY"),
                firstNonBlank(environment, "OPENAI_LLM_MODEL", "LLM_MODEL"),
                firstNonBlank(environment, "OPENAI_ASR_MODEL", "ASR_MODEL"),
                firstNonBlank(environment, "OPENAI_TTS_MODEL", "TTS_MODEL"),
                firstNonBlank(environment, "OPENAI_TTS_VOICE", "TTS_VOICE"),
                environment.getProperty("OPENAI_TIMEOUT", Duration.class, Duration.ofSeconds(30)));
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String llmModel() {
        return llmModel;
    }

    public String asrModel() {
        return asrModel;
    }

    public String ttsModel() {
        return ttsModel;
    }

    public String ttsVoice() {
        return ttsVoice;
    }

    public Duration timeout() {
        return timeout;
    }

    private static String firstNonBlank(Environment environment, String... keysOrFallback) {
        for (String key : keysOrFallback) {
            if (!key.contains("_")) {
                return key;
            }
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String requireNonBlank(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " must be configured for the OpenAI provider");
        }
        return value.trim();
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
