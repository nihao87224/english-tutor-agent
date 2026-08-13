package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.fake.FakeChatProvider;
import cn.forever24.tutor.ai.fake.FakeSpeechToTextProvider;
import cn.forever24.tutor.ai.fake.FakeTextToSpeechProvider;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AgentConfigurationTest {

    @Test
    void defaultConfigurationRegistersFakeProviders() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AgentConfiguration.class)) {
            assertInstanceOf(FakeChatProvider.class, context.getBean(ChatProvider.class));
            assertInstanceOf(FakeSpeechToTextProvider.class, context.getBean(SpeechToTextProvider.class));
            assertInstanceOf(FakeTextToSpeechProvider.class, context.getBean(TextToSpeechProvider.class));
            assertInstanceOf(CorrectionAnalyzer.class, context.getBean(CorrectionAnalyzer.class));
        }
    }
}
