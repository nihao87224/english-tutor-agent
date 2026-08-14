package cn.forever24.tutor.application.provider;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class AiProviderConfigurationApplicationService {

    private final AiProviderConfigurationRepository repository;
    private final Clock clock;

    public AiProviderConfigurationApplicationService(AiProviderConfigurationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<AiProviderConfiguration> listProviders() {
        return repository.list();
    }

    public AiProviderConfiguration getProvider(String providerCode) {
        return repository.findByCode(normalizeCode(providerCode))
                .orElseThrow(() -> AiProviderConfigurationException.notFound(providerCode));
    }

    public ActiveAiProviderConfiguration defaultLlmProvider() {
        return repository.requireDefault(AiProviderPurpose.LLM);
    }

    public ActiveAiProviderConfiguration defaultAsrProvider() {
        return repository.requireDefault(AiProviderPurpose.ASR);
    }

    public ActiveAiProviderConfiguration defaultTtsProvider() {
        return repository.requireDefault(AiProviderPurpose.TTS);
    }

    public AiProviderConfiguration saveProvider(
            String providerCode,
            String providerType,
            String displayName,
            boolean enabled,
            boolean defaultLlm,
            boolean defaultAsr,
            boolean defaultTts,
            String baseUrl,
            String llmModel,
            String asrModel,
            String ttsModel,
            String ttsVoice,
            Duration timeout
    ) {
        AiProviderType type = parseProviderType(providerType);
        if (type != AiProviderType.OPENAI) {
            throw AiProviderConfigurationException.invalid("Only OPENAI provider type is supported");
        }
        return repository.save(new AiProviderConfigurationDraft(
                normalizeCode(providerCode),
                type,
                requireNonBlank(displayName, "displayName"),
                enabled,
                defaultLlm,
                defaultAsr,
                defaultTts,
                URI.create(stripTrailingSlash(requireNonBlank(baseUrl, "baseUrl"))),
                requireNonBlank(llmModel, "llmModel"),
                requireNonBlank(asrModel, "asrModel"),
                requireNonBlank(ttsModel, "ttsModel"),
                requireNonBlank(ttsVoice, "ttsVoice"),
                timeout == null ? Duration.ofSeconds(30) : timeout),
                clock.instant());
    }

    public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId) {
        if (actorUserId <= 0) {
            throw AiProviderConfigurationException.invalid("actor user id is required");
        }
        return repository.replaceApiKey(
                normalizeCode(providerCode),
                requireNonBlank(rawApiKey, "apiKey"),
                actorUserId,
                clock.instant());
    }

    private static AiProviderType parseProviderType(String providerType) {
        try {
            return AiProviderType.valueOf(requireNonBlank(providerType, "providerType"));
        } catch (IllegalArgumentException exception) {
            throw AiProviderConfigurationException.invalid("Unsupported provider type: " + providerType);
        }
    }

    private static String normalizeCode(String providerCode) {
        String normalized = requireNonBlank(providerCode, "providerCode").trim().toLowerCase();
        if (!normalized.matches("[a-z0-9._-]{1,64}")) {
            throw AiProviderConfigurationException.invalid("providerCode must be 1-64 lowercase letters, digits, dot, underscore or dash");
        }
        return normalized;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw AiProviderConfigurationException.invalid(fieldName + " is required");
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
