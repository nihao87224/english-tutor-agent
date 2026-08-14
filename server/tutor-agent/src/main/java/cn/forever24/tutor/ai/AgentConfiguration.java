package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.conversation.ProviderConversationReplyStreamer;
import cn.forever24.tutor.ai.conversation.ProviderLayeredCorrectionAnalyzer;
import cn.forever24.tutor.ai.openai.OpenAiChatProvider;
import cn.forever24.tutor.ai.openai.OpenAiHttpClient;
import cn.forever24.tutor.ai.openai.OpenAiOpenAnswerEvaluator;
import cn.forever24.tutor.ai.openai.OpenAiProviderProperties;
import cn.forever24.tutor.ai.openai.OpenAiSpeechToTextProvider;
import cn.forever24.tutor.ai.openai.OpenAiTextToSpeechProvider;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AgentConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    OpenAiProviderProperties openAiProviderProperties(Environment environment) {
        return OpenAiProviderProperties.fromEnvironment(environment);
    }

    @Bean
    OpenAiHttpClient openAiHttpClient(ObjectMapper objectMapper, OpenAiProviderProperties properties) {
        return new OpenAiHttpClient(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(ChatProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.llm-provider", havingValue = "openai", matchIfMissing = true)
    ChatProvider openAiChatProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        return new OpenAiChatProvider(client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(SpeechToTextProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.asr-provider", havingValue = "openai", matchIfMissing = true)
    SpeechToTextProvider openAiSpeechToTextProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        return new OpenAiSpeechToTextProvider(client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(TextToSpeechProvider.class)
    @ConditionalOnProperty(name = "tutor.ai.tts-provider", havingValue = "openai", matchIfMissing = true)
    TextToSpeechProvider openAiTextToSpeechProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        return new OpenAiTextToSpeechProvider(client, properties);
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
