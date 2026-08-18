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
        Duration timeout,
        boolean defaultLlm,
        boolean defaultAsr,
        boolean defaultTts
) {

    public static AiProviderEnvironmentDefaults deepSeek(Environment environment) {
        String baseUrl = firstNonBlank(environment, "DEEPSEEK_BASE_URL");
        return new AiProviderEnvironmentDefaults(
                "deepseek",
                AiProviderType.OPENAI_COMPATIBLE,
                "DeepSeek",
                URI.create(stripTrailingSlash(baseUrl == null ? "https://api.deepseek.com" : baseUrl)),
                firstNonBlank(environment, "DEEPSEEK_API_KEY"),
                firstNonBlank(environment, "DEEPSEEK_LLM_MODEL"),
                null,
                null,
                null,
                environment.getProperty("DEEPSEEK_TIMEOUT", Duration.class, Duration.ofSeconds(30)),
                true,
                false,
                false);
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
