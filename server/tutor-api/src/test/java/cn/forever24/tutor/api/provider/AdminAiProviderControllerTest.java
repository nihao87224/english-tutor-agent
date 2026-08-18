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
import org.springframework.security.access.prepost.PreAuthorize;

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

    @Test
    void returnsConnectionTestFailureAsASecretSafeResponse() {
        AdminAiProviderController controller = new AdminAiProviderController(new AiProviderConfigurationApplicationService(
                new CapturingProviderRepository(),
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC),
                configuration -> cn.forever24.tutor.application.provider.AiProviderConnectionTestResult.failure(15, "INVALID_API_KEY")));

        AiProviderConnectionTestResponse response = controller.testConnection("openai");

        assertEquals(false, response.success());
        assertEquals(15, response.latencyMs());
        assertEquals("INVALID_API_KEY", response.error());
    }

    @Test
    void protectsConnectionTestingWithProviderManagementPermission() throws NoSuchMethodException {
        PreAuthorize authorization = AdminAiProviderController.class
                .getMethod("testConnection", String.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasAuthority('AI_PROVIDER_MANAGE')", authorization.value());
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
        public ActiveAiProviderConfiguration requireActive(String providerCode) {
            return new ActiveAiProviderConfiguration(
                    "openai",
                    AiProviderType.OPENAI,
                    URI.create("https://api.openai.com/v1"),
                    "test-key",
                    "test-model",
                    null,
                    null,
                    null,
                    Duration.ofSeconds(30));
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
