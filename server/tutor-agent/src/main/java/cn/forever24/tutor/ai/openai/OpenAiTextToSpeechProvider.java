package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.ai.provider.TtsResult;
import cn.forever24.tutor.ai.provider.VoiceOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

public final class OpenAiTextToSpeechProvider implements TextToSpeechProvider {

    private final OpenAiHttpClient client;
    private final OpenAiProviderProperties properties;

    public OpenAiTextToSpeechProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public TtsResult synthesize(String traceId, String text, VoiceOptions options) {
        ObjectNode body = client.objectMapper().createObjectNode();
        body.put("model", properties.ttsModel());
        body.put("voice", voice(options));
        body.put("input", cn.forever24.tutor.ai.provider.ProviderText.requireNonBlank(text, "text"));
        body.put("response_format", responseFormat(options.audioFormat()));

        OpenAiHttpClient.BinaryResponse response = client.postJsonForBytes("/audio/speech", body);
        return new TtsResult(
                response.body(),
                options.audioFormat(),
                estimatedDuration(text),
                new ProviderTrace(traceId, OpenAiProviderProperties.PROVIDER_ID, properties.ttsModel(), "openai-tts-v1", "openai-speech-bytes-v1"),
                new ProviderUsage(0, 0, 0, estimatedDuration(text).toMillis(), BigDecimal.ZERO));
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                OpenAiProviderProperties.PROVIDER_ID,
                properties.ttsModel(),
                Set.of(ProviderCapability.TEXT_TO_SPEECH),
                properties.timeout());
    }

    private String voice(VoiceOptions options) {
        if (options.voiceId() == null || options.voiceId().isBlank() || "english-neutral".equals(options.voiceId())) {
            return properties.ttsVoice();
        }
        return options.voiceId();
    }

    private String responseFormat(String contentType) {
        return switch (contentType) {
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/ogg" -> "opus";
            default -> "mp3";
        };
    }

    private Duration estimatedDuration(String text) {
        return Duration.ofMillis(Math.max(500L, text.length() * 55L));
    }
}
