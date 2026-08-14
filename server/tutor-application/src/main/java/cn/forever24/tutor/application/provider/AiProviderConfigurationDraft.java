package cn.forever24.tutor.application.provider;

import java.net.URI;
import java.time.Duration;

public record AiProviderConfigurationDraft(
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
        Duration timeout
) {
}
