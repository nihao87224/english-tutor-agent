package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.ai.openai.OpenAiHttpClient;
import cn.forever24.tutor.ai.openai.OpenAiProviderProperties;
import cn.forever24.tutor.ai.openai.OpenAiSpeechToTextProvider;
import cn.forever24.tutor.ai.provider.AsrOptions;
import cn.forever24.tutor.ai.provider.AsrResult;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuntimeOpenAiSpeechToTextProvider implements SpeechToTextProvider {

    private final AiProviderConfigurationApplicationService configurationService;
    private final ObjectMapper objectMapper;

    public RuntimeOpenAiSpeechToTextProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AsrResult transcribe(AudioInput input, AsrOptions options) {
        return delegate().transcribe(input, options);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return delegate().capabilities();
    }

    private OpenAiSpeechToTextProvider delegate() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.fromActiveConfiguration(configurationService.defaultAsrProvider());
        return new OpenAiSpeechToTextProvider(new OpenAiHttpClient(objectMapper, properties), properties);
    }
}
