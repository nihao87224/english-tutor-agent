package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiChatProvider;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiSpeechToTextProvider;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiTextToSpeechProvider;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;
import cn.forever24.tutor.application.provider.AiProviderType;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AgentConfigurationTest {

    @Test
    void defaultConfigurationRegistersRuntimeProviders() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProviderConfigurationApplicationService.class, AgentConfigurationTest::configurationService);
            context.register(AgentConfiguration.class);
            context.refresh();

            assertInstanceOf(RuntimeOpenAiChatProvider.class, context.getBean(ChatProvider.class));
            assertInstanceOf(RuntimeOpenAiSpeechToTextProvider.class, context.getBean(SpeechToTextProvider.class));
            assertInstanceOf(RuntimeOpenAiTextToSpeechProvider.class, context.getBean(TextToSpeechProvider.class));
            assertInstanceOf(CorrectionAnalyzer.class, context.getBean(CorrectionAnalyzer.class));
        }
    }

    @Test
    void legacyProviderSelectionPropertiesCannotDisableRuntimeProviders() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getSystemProperties().put("tutor.ai.llm-provider", "legacy");
            context.getEnvironment().getSystemProperties().put("tutor.ai.asr-provider", "legacy");
            context.getEnvironment().getSystemProperties().put("tutor.ai.tts-provider", "legacy");
            context.registerBean(AiProviderConfigurationApplicationService.class, AgentConfigurationTest::configurationService);
            context.register(AgentConfiguration.class);
            context.refresh();

            assertInstanceOf(RuntimeOpenAiChatProvider.class, context.getBean(ChatProvider.class));
            assertInstanceOf(RuntimeOpenAiSpeechToTextProvider.class, context.getBean(SpeechToTextProvider.class));
            assertInstanceOf(RuntimeOpenAiTextToSpeechProvider.class, context.getBean(TextToSpeechProvider.class));
        }
    }

    private static AiProviderConfigurationApplicationService configurationService() {
        return new AiProviderConfigurationApplicationService(new AiProviderConfigurationRepository() {
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
                return new ActiveAiProviderConfiguration(
                        "openai",
                        AiProviderType.OPENAI,
                        URI.create("https://api.openai.com/v1"),
                        "test-key",
                        "test-model",
                        "test-asr-model",
                        "test-tts-model",
                        "test-voice",
                        Duration.ofSeconds(30));
            }

            @Override
            public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
                throw new UnsupportedOperationException();
            }
        }, Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC));
    }
}
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
