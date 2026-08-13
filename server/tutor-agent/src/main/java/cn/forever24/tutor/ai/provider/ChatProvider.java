package cn.forever24.tutor.ai.provider;

public interface ChatProvider {

    ChatStream stream(ChatProviderRequest request);

    StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema);

    ProviderCapabilities capabilities();
}
