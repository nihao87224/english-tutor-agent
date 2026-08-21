package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.AsrOptions;
import cn.forever24.tutor.ai.provider.AsrResult;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OpenAiSpeechToTextProvider implements SpeechToTextProvider {

    private final OpenAiHttpClient client;
    private final OpenAiProviderProperties properties;

    public OpenAiSpeechToTextProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public AsrResult transcribe(AudioInput input, AsrOptions options) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", properties.asrModel());
        fields.put("language", languageCode(options.languageTag()));
        fields.put("response_format", "json");
        JsonNode response = client.postMultipart(
                "/audio/transcriptions",
                fields,
                input.content(),
                "audio." + extension(input.contentType()),
                input.contentType());
        return new AsrResult(
                response.path("text").asText(),
                confidence(response),
                new ProviderTrace(input.traceId(), OpenAiProviderProperties.PROVIDER_ID, properties.asrModel(), "openai-asr-v1", "openai-transcription-json-v1"),
                new ProviderUsage(0, 0, input.duration().toMillis(), 0, BigDecimal.ZERO));
    }

    static double confidence(JsonNode response) {
        JsonNode value = response.path("confidence");
        if (!value.isNumber()) {
            // The standard JSON response does not guarantee a confidence score.
            // Treat unknown as low confidence so user speech cannot bypass confirmation.
            return 0.0;
        }
        double confidence = value.asDouble();
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new cn.forever24.tutor.ai.provider.AiProviderException(
                    cn.forever24.tutor.ai.provider.AiProviderErrorType.INVALID_OUTPUT,
                    "ASR confidence must be between 0 and 1");
        }
        return confidence;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                OpenAiProviderProperties.PROVIDER_ID,
                properties.asrModel(),
                Set.of(ProviderCapability.SPEECH_TO_TEXT),
                properties.timeout());
    }

    private String languageCode(String languageTag) {
        int separator = languageTag.indexOf('-');
        return separator < 0 ? languageTag : languageTag.substring(0, separator);
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "audio/mpeg" -> "mp3";
            case "audio/mp4" -> "mp4";
            case "audio/webm" -> "webm";
            case "audio/ogg" -> "ogg";
            case "audio/x-wav", "audio/wav" -> "wav";
            default -> "wav";
        };
    }
}
