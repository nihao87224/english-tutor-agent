package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAiProviderConfigurationRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void replacesSecretWithMaskedHintAndResolvesActiveProvider() {
        AiProviderConfigurationApplicationService service = service("initial-model");

        AiProviderConfiguration updated = service.replaceApiKey("openai", "sk-real-provider-key", 1001L);

        assertTrue(updated.apiKeyConfigured());
        assertEquals("****-key", updated.apiKeyMaskedHint());
        assertEquals("sk-real-provider-key", service.defaultLlmProvider().apiKey());
    }

    @Test
    void changedDefaultModelIsVisibleOnNextResolution() {
        AiProviderConfigurationApplicationService service = service("initial-model");

        service.saveProvider(
                "openai",
                AiProviderType.OPENAI.name(),
                "OpenAI",
                true,
                true,
                true,
                true,
                "https://api.openai.com/v1",
                "next-model",
                "next-asr",
                "next-tts",
                "next-voice",
                Duration.ofSeconds(45));

        assertEquals("next-model", service.defaultLlmProvider().llmModel());
        assertEquals(Duration.ofSeconds(45), service.defaultTtsProvider().timeout());
    }

    private static AiProviderConfigurationApplicationService service(String llmModel) {
        AesGcmSecretCipher cipher = new AesGcmSecretCipher("0123456789abcdef0123456789abcdef", "test");
        AiProviderEnvironmentDefaults defaults = new AiProviderEnvironmentDefaults(
                "openai",
                AiProviderType.OPENAI,
                "OpenAI",
                URI.create("https://api.openai.com/v1"),
                "sk-bootstrap-key",
                llmModel,
                "test-asr",
                "test-tts",
                "test-voice",
                Duration.ofSeconds(30),
                true,
                true,
                true);
        return new AiProviderConfigurationApplicationService(
                new InMemoryAiProviderConfigurationRepository(cipher, defaults),
                CLOCK);
    }
}
