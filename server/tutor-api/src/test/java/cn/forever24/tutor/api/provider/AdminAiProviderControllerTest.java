package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;
import cn.forever24.tutor.application.provider.AiProviderType;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAiProviderControllerTest {

    @Test
    void replacesSecretWithoutReturningRawApiKey() {
        CapturingProviderRepository repository = new CapturingProviderRepository();
        AdminAiProviderController controller = new AdminAiProviderController(new AiProviderConfigurationApplicationService(
                repository,
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC)));

        AiProviderResponse response = controller.replaceApiKey(
                "openai",
                new AiProviderSecretRequest("sk-real-provider-secret"),
                new TestingAuthenticationToken("1001", "ignored"));

        assertEquals("sk-real-provider-secret", repository.capturedRawApiKey);
        assertTrue(response.apiKeyConfigured());
        assertEquals("****cret", response.apiKeyMaskedHint());
    }

    private static final class CapturingProviderRepository implements AiProviderConfigurationRepository {

        private String capturedRawApiKey;

        @Override
        public List<AiProviderConfiguration> list() {
            return List.of(configuration(false, null));
        }

        @Override
        public Optional<AiProviderConfiguration> findByCode(String providerCode) {
            return Optional.of(configuration(false, null));
        }

        @Override
        public ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
            capturedRawApiKey = rawApiKey;
            return configuration(true, "****cret");
        }

        private static AiProviderConfiguration configuration(boolean secretConfigured, String maskedHint) {
            return new AiProviderConfiguration(
                    "openai",
                    AiProviderType.OPENAI,
                    "OpenAI",
                    true,
                    true,
                    true,
                    true,
                    URI.create("https://api.openai.com/v1"),
                    "chat-model",
                    "asr-model",
                    "tts-model",
                    "voice",
                    Duration.ofSeconds(30),
                    secretConfigured,
                    maskedHint);
        }
    }
}
