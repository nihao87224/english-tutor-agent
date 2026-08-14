package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.ai.openai.OpenAiHttpClient;
import cn.forever24.tutor.ai.openai.OpenAiProviderProperties;
import cn.forever24.tutor.ai.openai.OpenAiTextToSpeechProvider;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.ai.provider.TtsResult;
import cn.forever24.tutor.ai.provider.VoiceOptions;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuntimeOpenAiTextToSpeechProvider implements TextToSpeechProvider {

    private final AiProviderConfigurationApplicationService configurationService;
    private final ObjectMapper objectMapper;

    public RuntimeOpenAiTextToSpeechProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TtsResult synthesize(String traceId, String text, VoiceOptions options) {
        return delegate().synthesize(traceId, text, options);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return delegate().capabilities();
    }

    private OpenAiTextToSpeechProvider delegate() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.fromActiveConfiguration(configurationService.defaultTtsProvider());
        return new OpenAiTextToSpeechProvider(new OpenAiHttpClient(objectMapper, properties), properties);
    }
}
