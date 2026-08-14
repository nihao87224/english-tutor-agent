package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderType;

import java.net.URI;

public record AiProviderUpdateRequest(
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
        Long timeoutSeconds
) {
}
