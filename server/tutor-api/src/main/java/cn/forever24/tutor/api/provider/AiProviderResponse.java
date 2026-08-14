package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderType;

import java.net.URI;

public record AiProviderResponse(
        String providerCode,
        AiProviderType providerType,
        String displayName,
        boolean enabled,
        boolean defaultLlm,
        boolean defaultAsr,
        boolean defaultTts,
        URI baseUrl,
        String llmModel,
        String asrModel,
        String ttsModel,
        String ttsVoice,
        long timeoutSeconds,
        boolean apiKeyConfigured,
        String apiKeyMaskedHint
) {

    static AiProviderResponse from(AiProviderConfiguration configuration) {
        return new AiProviderResponse(
                configuration.providerCode(),
                configuration.providerType(),
                configuration.displayName(),
                configuration.enabled(),
                configuration.defaultLlm(),
                configuration.defaultAsr(),
                configuration.defaultTts(),
                configuration.baseUrl(),
                configuration.llmModel(),
                configuration.asrModel(),
                configuration.ttsModel(),
                configuration.ttsVoice(),
                configuration.timeout().toSeconds(),
                configuration.apiKeyConfigured(),
                configuration.apiKeyMaskedHint());
    }
}
