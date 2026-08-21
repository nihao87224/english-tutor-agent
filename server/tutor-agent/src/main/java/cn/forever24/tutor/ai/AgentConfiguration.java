package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.conversation.ProviderConversationReplyStreamer;
import cn.forever24.tutor.ai.audio.ProviderAudioTranscriber;
import cn.forever24.tutor.ai.conversation.ProviderLayeredCorrectionAnalyzer;
import cn.forever24.tutor.ai.openai.OpenAiOpenAnswerEvaluator;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiChatProvider;
import cn.forever24.tutor.ai.runtime.RuntimeAiProviderConnectionTester;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiSpeechToTextProvider;
import cn.forever24.tutor.ai.runtime.RuntimeOpenAiTextToSpeechProvider;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.audio.AudioTranscriber;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderConnectionTester;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AgentConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean(AiProviderConnectionTester.class)
    AiProviderConnectionTester aiProviderConnectionTester(ObjectMapper objectMapper) {
        return new RuntimeAiProviderConnectionTester(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ChatProvider.class)
    ChatProvider openAiChatProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        return new RuntimeOpenAiChatProvider(configurationService, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SpeechToTextProvider.class)
    SpeechToTextProvider openAiSpeechToTextProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        return new RuntimeOpenAiSpeechToTextProvider(configurationService, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AudioTranscriber.class)
    AudioTranscriber audioTranscriber(SpeechToTextProvider provider) {
        return new ProviderAudioTranscriber(provider);
    }

    @Bean
    @ConditionalOnMissingBean(TextToSpeechProvider.class)
    TextToSpeechProvider openAiTextToSpeechProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        return new RuntimeOpenAiTextToSpeechProvider(configurationService, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(OpenAnswerEvaluator.class)
    OpenAnswerEvaluator openAiOpenAnswerEvaluator(ChatProvider chatProvider, ObjectMapper objectMapper) {
        return new OpenAiOpenAnswerEvaluator(chatProvider, objectMapper);
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
