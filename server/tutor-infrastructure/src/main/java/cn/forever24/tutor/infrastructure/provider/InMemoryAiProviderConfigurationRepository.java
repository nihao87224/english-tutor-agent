package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationException;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAiProviderConfigurationRepository implements AiProviderConfigurationRepository {

    private final AesGcmSecretCipher secretCipher;
    private final Map<String, StoredProvider> providers = new LinkedHashMap<>();
    private final Map<String, EncryptedSecret> apiKeySecrets = new LinkedHashMap<>();
    private final Map<String, String> maskedHints = new LinkedHashMap<>();

    public InMemoryAiProviderConfigurationRepository(AesGcmSecretCipher secretCipher, AiProviderEnvironmentDefaults defaults) {
        this.secretCipher = secretCipher;
        StoredProvider configuredProvider = StoredProvider.fromDefaults(defaults);
        providers.put(configuredProvider.providerCode, configuredProvider);
        if (defaults.apiKey() != null && !defaults.apiKey().isBlank()) {
            apiKeySecrets.put(configuredProvider.providerCode, secretCipher.encrypt(defaults.apiKey()));
            maskedHints.put(configuredProvider.providerCode, ProviderSecretMask.mask(defaults.apiKey()));
        }
    }

    @Override
    public synchronized List<AiProviderConfiguration> list() {
        return providers.values().stream()
                .sorted(Comparator.comparing(provider -> provider.providerCode))
                .map(this::toConfiguration)
                .toList();
    }

    @Override
    public synchronized Optional<AiProviderConfiguration> findByCode(String providerCode) {
        return Optional.ofNullable(providers.get(providerCode)).map(this::toConfiguration);
    }

    @Override
    public synchronized ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose) {
        StoredProvider provider = providers.values().stream()
                .filter(StoredProvider::enabled)
                .filter(candidate -> candidate.isDefaultFor(purpose))
                .findFirst()
                .orElseThrow(() -> AiProviderConfigurationException.unavailable("No enabled default AI provider for " + purpose.name()));
        EncryptedSecret secret = apiKeySecrets.get(provider.providerCode);
        if (secret == null) {
            throw AiProviderConfigurationException.unavailable("API key is not configured for provider " + provider.providerCode);
        }
        return provider.toActive(secretCipher.decrypt(secret));
    }

    @Override
    public synchronized AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
        StoredProvider provider = StoredProvider.fromDraft(draft);
        providers.put(provider.providerCode, provider);
        if (provider.defaultLlm) {
            unsetDefault(provider.providerCode, AiProviderPurpose.LLM);
        }
        if (provider.defaultAsr) {
            unsetDefault(provider.providerCode, AiProviderPurpose.ASR);
        }
        if (provider.defaultTts) {
            unsetDefault(provider.providerCode, AiProviderPurpose.TTS);
        }
        return toConfiguration(provider);
    }

    @Override
    public synchronized AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
        StoredProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw AiProviderConfigurationException.notFound(providerCode);
        }
        apiKeySecrets.put(providerCode, secretCipher.encrypt(rawApiKey));
        maskedHints.put(providerCode, ProviderSecretMask.mask(rawApiKey));
        return toConfiguration(provider);
    }

    private void unsetDefault(String providerCode, AiProviderPurpose purpose) {
        List<StoredProvider> updated = new ArrayList<>();
        for (StoredProvider provider : providers.values()) {
            if (!provider.providerCode.equals(providerCode) && provider.isDefaultFor(purpose)) {
                updated.add(provider.withDefault(purpose, false));
            }
        }
        for (StoredProvider provider : updated) {
            providers.put(provider.providerCode, provider);
        }
    }

    private AiProviderConfiguration toConfiguration(StoredProvider provider) {
        boolean secretConfigured = apiKeySecrets.containsKey(provider.providerCode);
        return new AiProviderConfiguration(
                provider.providerCode,
                provider.providerType,
                provider.displayName,
                provider.enabled,
                provider.defaultLlm,
                provider.defaultAsr,
                provider.defaultTts,
                provider.baseUrl,
                provider.llmModel,
                provider.asrModel,
                provider.ttsModel,
                provider.ttsVoice,
                provider.timeout,
                secretConfigured,
                secretConfigured ? maskedHints.get(provider.providerCode) : null);
    }

    private record StoredProvider(
            String providerCode,
            cn.forever24.tutor.application.provider.AiProviderType providerType,
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

        static StoredProvider fromDefaults(AiProviderEnvironmentDefaults defaults) {
            return new StoredProvider(
                    defaults.providerCode(),
                    defaults.providerType(),
                    defaults.displayName(),
                    defaults.defaultLlm(),
                    defaults.defaultAsr(),
                    defaults.defaultTts(),
                    true,
                    defaults.baseUrl(),
                    defaults.llmModel(),
                    defaults.asrModel(),
                    defaults.ttsModel(),
                    defaults.ttsVoice(),
                    defaults.timeout());
        }

        static StoredProvider fromDraft(AiProviderConfigurationDraft draft) {
            return new StoredProvider(
                    draft.providerCode(),
                    draft.providerType(),
                    draft.displayName(),
                    draft.enabled(),
                    draft.defaultLlm(),
                    draft.defaultAsr(),
                    draft.defaultTts(),
                    draft.baseUrl(),
                    draft.llmModel(),
                    draft.asrModel(),
                    draft.ttsModel(),
                    draft.ttsVoice(),
                    draft.timeout());
        }

        ActiveAiProviderConfiguration toActive(String apiKey) {
            return new ActiveAiProviderConfiguration(
                    providerCode,
                    providerType,
                    baseUrl,
                    apiKey,
                    llmModel,
                    asrModel,
                    ttsModel,
                    ttsVoice,
                    timeout);
        }

        boolean isDefaultFor(AiProviderPurpose purpose) {
            return switch (purpose) {
                case LLM -> defaultLlm;
                case ASR -> defaultAsr;
                case TTS -> defaultTts;
            };
        }

        StoredProvider withDefault(AiProviderPurpose purpose, boolean value) {
            return new StoredProvider(
                    providerCode,
                    providerType,
                    displayName,
                    enabled,
                    purpose == AiProviderPurpose.LLM ? value : defaultLlm,
                    purpose == AiProviderPurpose.ASR ? value : defaultAsr,
                    purpose == AiProviderPurpose.TTS ? value : defaultTts,
                    baseUrl,
                    llmModel,
                    asrModel,
                    ttsModel,
                    ttsVoice,
                    timeout);
        }
    }
}
