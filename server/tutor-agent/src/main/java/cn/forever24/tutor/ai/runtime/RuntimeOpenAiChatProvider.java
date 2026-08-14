package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.ai.openai.OpenAiChatProvider;
import cn.forever24.tutor.ai.openai.OpenAiHttpClient;
import cn.forever24.tutor.ai.openai.OpenAiProviderProperties;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuntimeOpenAiChatProvider implements ChatProvider {

    private final AiProviderConfigurationApplicationService configurationService;
    private final ObjectMapper objectMapper;

    public RuntimeOpenAiChatProvider(AiProviderConfigurationApplicationService configurationService, ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatStream stream(ChatProviderRequest request) {
        return delegate().stream(request);
    }

    @Override
    public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
        return delegate().completeStructured(request, schema);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return delegate().capabilities();
    }

    private OpenAiChatProvider delegate() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.fromActiveConfiguration(configurationService.defaultLlmProvider());
        return new OpenAiChatProvider(new OpenAiHttpClient(objectMapper, properties), properties);
    }
}
