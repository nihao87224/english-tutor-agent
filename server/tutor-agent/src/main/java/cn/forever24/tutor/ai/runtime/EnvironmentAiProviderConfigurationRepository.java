package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationException;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;
import cn.forever24.tutor.application.provider.AiProviderType;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class EnvironmentAiProviderConfigurationRepository implements AiProviderConfigurationRepository {

    private final Environment environment;

    public EnvironmentAiProviderConfigurationRepository(Environment environment) {
        this.environment = environment;
    }

    @Override
    public List<AiProviderConfiguration> list() {
        return List.of(configuration());
    }

    @Override
    public Optional<AiProviderConfiguration> findByCode(String providerCode) {
        return "deepseek".equals(providerCode) ? Optional.of(configuration()) : Optional.empty();
    }

    @Override
    public ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose) {
        if (purpose != AiProviderPurpose.LLM) {
            throw AiProviderConfigurationException.unavailable("No environment default AI provider for " + purpose.name());
        }
        return activeConfiguration();
    }

    @Override
    public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
        throw AiProviderConfigurationException.unavailable("Provider persistence is not configured");
    }

    @Override
    public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
        throw AiProviderConfigurationException.unavailable("Provider secret persistence is not configured");
    }

    private AiProviderConfiguration configuration() {
        ActiveAiProviderConfiguration active = activeConfiguration();
        return new AiProviderConfiguration(
                active.providerCode(),
                active.providerType(),
                "DeepSeek",
                true,
                true,
                false,
                false,
                active.baseUrl(),
                active.llmModel(),
                active.asrModel(),
                active.ttsModel(),
                active.ttsVoice(),
                active.timeout(),
                true,
                mask(active.apiKey()));
    }

    private ActiveAiProviderConfiguration activeConfiguration() {
        String baseUrl = firstNonBlank("DEEPSEEK_BASE_URL");
        return new ActiveAiProviderConfiguration(
                "deepseek",
                AiProviderType.OPENAI_COMPATIBLE,
                URI.create(stripTrailingSlash(baseUrl == null ? "https://api.deepseek.com" : baseUrl)),
                requireNonBlank(firstNonBlank("DEEPSEEK_API_KEY"), "DEEPSEEK_API_KEY"),
                requireNonBlank(firstNonBlank("DEEPSEEK_LLM_MODEL"), "DEEPSEEK_LLM_MODEL"),
                null,
                null,
                null,
                environment.getProperty("DEEPSEEK_TIMEOUT", Duration.class, Duration.ofSeconds(30)));
    }

    private String firstNonBlank(String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String requireNonBlank(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw AiProviderConfigurationException.unavailable(envName + " must be configured for the DeepSeek provider");
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

    private static String mask(String secret) {
        int visibleChars = Math.min(4, secret.length());
        return "****" + secret.substring(secret.length() - visibleChars);
    }
}
