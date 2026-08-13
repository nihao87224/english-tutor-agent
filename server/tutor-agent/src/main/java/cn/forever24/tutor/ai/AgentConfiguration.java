package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.conversation.ProviderConversationReplyStreamer;
import cn.forever24.tutor.ai.conversation.ProviderLayeredCorrectionAnalyzer;
import cn.forever24.tutor.ai.fake.FakeOpenAnswerEvaluator;
import cn.forever24.tutor.ai.fake.FakeChatProvider;
import cn.forever24.tutor.ai.fake.FakeSpeechToTextProvider;
import cn.forever24.tutor.ai.fake.FakeTextToSpeechProvider;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.llm-provider", havingValue = "fake", matchIfMissing = true)
    ChatProvider fakeChatProvider() {
        return new FakeChatProvider();
    }

    @Bean
    @ConditionalOnMissingBean(SpeechToTextProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.asr-provider", havingValue = "fake", matchIfMissing = true)
    SpeechToTextProvider fakeSpeechToTextProvider() {
        return new FakeSpeechToTextProvider();
    }

    @Bean
    @ConditionalOnMissingBean(TextToSpeechProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.tts-provider", havingValue = "fake", matchIfMissing = true)
    TextToSpeechProvider fakeTextToSpeechProvider() {
        return new FakeTextToSpeechProvider();
    }

    @Bean
    @ConditionalOnMissingBean(OpenAnswerEvaluator.class)
    @ConditionalOnProperty(name = "tutor.ai.llm-provider", havingValue = "fake", matchIfMissing = true)
    OpenAnswerEvaluator fakeOpenAnswerEvaluator() {
        return new FakeOpenAnswerEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean(CorrectionAnalyzer.class)
    CorrectionAnalyzer correctionAnalyzer(ChatProvider chatProvider) {
        return new ProviderLayeredCorrectionAnalyzer(chatProvider);
    }

    @Bean
    @ConditionalOnMissingBean(ConversationReplyStreamer.class)
    ConversationReplyStreamer conversationReplyStreamer(ChatProvider chatProvider, CorrectionAnalyzer correctionAnalyzer) {
        return new ProviderConversationReplyStreamer(chatProvider, correctionAnalyzer);
    }
}
