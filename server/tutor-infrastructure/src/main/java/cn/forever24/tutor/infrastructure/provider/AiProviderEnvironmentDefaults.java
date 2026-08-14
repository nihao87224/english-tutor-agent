package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.AiProviderType;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.time.Duration;

public record AiProviderEnvironmentDefaults(
        String providerCode,
        AiProviderType providerType,
        String displayName,
        URI baseUrl,
        String apiKey,
        String llmModel,
        String asrModel,
        String ttsModel,
        String ttsVoice,
        Duration timeout
) {

    public static AiProviderEnvironmentDefaults openAi(Environment environment) {
        String baseUrl = firstNonBlank(environment, "OPENAI_BASE_URL", "LLM_BASE_URL");
        return new AiProviderEnvironmentDefaults(
                "openai",
                AiProviderType.OPENAI,
                "OpenAI",
                URI.create(stripTrailingSlash(baseUrl == null ? "https://api.openai.com/v1" : baseUrl)),
                firstNonBlank(environment, "OPENAI_API_KEY", "LLM_API_KEY"),
                firstNonBlank(environment, "OPENAI_LLM_MODEL", "LLM_MODEL"),
                firstNonBlank(environment, "OPENAI_ASR_MODEL", "ASR_MODEL"),
                firstNonBlank(environment, "OPENAI_TTS_MODEL", "TTS_MODEL"),
                firstNonBlank(environment, "OPENAI_TTS_VOICE", "TTS_VOICE"),
                environment.getProperty("OPENAI_TIMEOUT", Duration.class, Duration.ofSeconds(30)));
    }

    private static String firstNonBlank(Environment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
