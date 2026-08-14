package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.openai.OpenAiChatProvider;
import cn.forever24.tutor.ai.openai.OpenAiSpeechToTextProvider;
import cn.forever24.tutor.ai.openai.OpenAiTextToSpeechProvider;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AgentConfigurationTest {

    @Test
    void defaultConfigurationRegistersOpenAiProviders() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getSystemProperties().put("OPENAI_API_KEY", "test-key");
            context.getEnvironment().getSystemProperties().put("OPENAI_LLM_MODEL", "test-chat-model");
            context.getEnvironment().getSystemProperties().put("OPENAI_ASR_MODEL", "test-asr-model");
            context.getEnvironment().getSystemProperties().put("OPENAI_TTS_MODEL", "test-tts-model");
            context.getEnvironment().getSystemProperties().put("OPENAI_TTS_VOICE", "test-voice");
            context.register(AgentConfiguration.class);
            context.refresh();

            assertInstanceOf(OpenAiChatProvider.class, context.getBean(ChatProvider.class));
            assertInstanceOf(OpenAiSpeechToTextProvider.class, context.getBean(SpeechToTextProvider.class));
            assertInstanceOf(OpenAiTextToSpeechProvider.class, context.getBean(TextToSpeechProvider.class));
            assertInstanceOf(CorrectionAnalyzer.class, context.getBean(CorrectionAnalyzer.class));
        }
    }
}
