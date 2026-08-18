package cn.forever24.tutor.application.provider;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderConfigurationApplicationServiceTest {

    @Test
    void acceptsDeepSeekAsOpenAiCompatibleLlmProviderWithoutSpeechModels() {
        CapturingRepository repository = new CapturingRepository();
        AiProviderConfigurationApplicationService service = service(repository);

        service.saveProvider(
                "deepseek", "OPENAI_COMPATIBLE", "DeepSeek", true, true, false, false,
                "https://api.deepseek.com", "deepseek-v4-flash", null, null, null, Duration.ofSeconds(30));

        assertEquals(AiProviderType.OPENAI_COMPATIBLE, repository.draft.providerType());
        assertEquals("deepseek-v4-flash", repository.draft.llmModel());
        assertEquals(null, repository.draft.asrModel());
    }

    @Test
    void rejectsSpeechDefaultsForProvidersWithoutSpeechProtocolAdapters() {
        AiProviderConfigurationApplicationService service = service(new CapturingRepository());

        AiProviderConfigurationException exception = assertThrows(AiProviderConfigurationException.class, () -> service.saveProvider(
                "gemini", "GEMINI", "Gemini", true, true, true, false,
                "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", "unused", null, null, Duration.ofSeconds(30)));

        assertEquals("INVALID_AI_PROVIDER_CONFIGURATION", exception.code());
    }

    private static AiProviderConfigurationApplicationService service(CapturingRepository repository) {
        return new AiProviderConfigurationApplicationService(
                repository,
                Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class CapturingRepository implements AiProviderConfigurationRepository {

        private AiProviderConfigurationDraft draft;

        @Override
        public List<AiProviderConfiguration> list() {
            return List.of();
        }

        @Override
        public Optional<AiProviderConfiguration> findByCode(String providerCode) {
            return Optional.empty();
        }

        @Override
        public ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
            this.draft = draft;
            return new AiProviderConfiguration(
                    draft.providerCode(), draft.providerType(), draft.displayName(), draft.enabled(), draft.defaultLlm(),
                    draft.defaultAsr(), draft.defaultTts(), draft.baseUrl(), draft.llmModel(), draft.asrModel(),
                    draft.ttsModel(), draft.ttsVoice(), draft.timeout(), false, null);
        }

        @Override
        public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
            throw new UnsupportedOperationException();
        }
    }
}
