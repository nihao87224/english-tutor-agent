package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;
import cn.forever24.tutor.application.provider.AiProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeOpenAiChatProviderTest {

    @Test
    void capabilitiesResolveCurrentDefaultProviderConfiguration() {
        MutableProviderRepository repository = new MutableProviderRepository("first-model", Duration.ofSeconds(30));
        AiProviderConfigurationApplicationService service = new AiProviderConfigurationApplicationService(
                repository,
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));
        RuntimeOpenAiChatProvider provider = new RuntimeOpenAiChatProvider(service, new ObjectMapper());

        service.saveProvider(
                "openai",
                AiProviderType.OPENAI.name(),
                "OpenAI",
                true,
                true,
                true,
                true,
                "https://api.openai.com/v1",
                "second-model",
                "asr-model",
                "tts-model",
                "voice",
                Duration.ofSeconds(60));

        ProviderCapabilities capabilities = provider.capabilities();

        assertEquals("second-model", capabilities.modelId());
        assertEquals(Duration.ofSeconds(60), capabilities.timeout());
    }

    @Test
    void capabilitiesResolveOpenAiCompatibleDefaultProvider() {
        MutableProviderRepository repository = new MutableProviderRepository("first-model", Duration.ofSeconds(30));
        AiProviderConfigurationApplicationService service = new AiProviderConfigurationApplicationService(
                repository,
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));
        RuntimeOpenAiChatProvider provider = new RuntimeOpenAiChatProvider(service, new ObjectMapper());

        service.saveProvider(
                "deepseek",
                AiProviderType.OPENAI_COMPATIBLE.name(),
                "DeepSeek",
                true,
                true,
                false,
                false,
                "https://api.deepseek.com",
                "deepseek-v4-flash",
                null,
                null,
                null,
                Duration.ofSeconds(45));

        ProviderCapabilities capabilities = provider.capabilities();

        assertEquals("deepseek", capabilities.providerId());
        assertEquals("deepseek-v4-flash", capabilities.modelId());
    }

    private static final class MutableProviderRepository implements AiProviderConfigurationRepository {

        private ActiveAiProviderConfiguration active;

        private MutableProviderRepository(String llmModel, Duration timeout) {
            this.active = active(llmModel, timeout);
        }

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
            return active;
        }

        @Override
        public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
            active = active(draft);
            return new AiProviderConfiguration(
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
                    draft.timeout(),
                    true,
                    "****test");
        }

        @Override
        public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
            throw new UnsupportedOperationException();
        }

        private static ActiveAiProviderConfiguration active(String llmModel, Duration timeout) {
            return new ActiveAiProviderConfiguration(
                    "openai",
                    AiProviderType.OPENAI,
                    URI.create("https://api.openai.com/v1"),
                    "sk-test",
                    llmModel,
                    "asr-model",
                    "tts-model",
                    "voice",
                    timeout);
        }

        private static ActiveAiProviderConfiguration active(AiProviderConfigurationDraft draft) {
            return new ActiveAiProviderConfiguration(
                    draft.providerCode(),
                    draft.providerType(),
                    draft.baseUrl(),
                    "sk-test",
                    draft.llmModel(),
                    draft.asrModel(),
                    draft.ttsModel(),
                    draft.ttsVoice(),
                    draft.timeout());
        }
    }
}
