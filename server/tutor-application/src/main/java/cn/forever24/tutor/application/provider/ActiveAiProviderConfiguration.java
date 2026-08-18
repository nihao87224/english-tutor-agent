package cn.forever24.tutor.application.provider;

import java.net.URI;
import java.time.Duration;

public record ActiveAiProviderConfiguration(
        String providerCode,
        AiProviderType providerType,
        URI baseUrl,
        String apiKey,
        String llmModel,
        String asrModel,
        String ttsModel,
        String ttsVoice,
        Duration timeout
) {

    public ActiveAiProviderConfiguration {
        providerCode = requireNonBlank(providerCode, "providerCode");
        if (providerType == null) {
            throw new IllegalArgumentException("providerType is required");
        }
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        apiKey = requireNonBlank(apiKey, "apiKey");
        llmModel = normalizeOptional(llmModel);
        asrModel = normalizeOptional(asrModel);
        ttsModel = normalizeOptional(ttsModel);
        ttsVoice = normalizeOptional(ttsVoice);
        timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
