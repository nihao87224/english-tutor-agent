package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.fake.FakeChatProvider;
import cn.forever24.tutor.ai.fake.FakeProviderVersions;
import cn.forever24.tutor.ai.fake.FakeSpeechToTextProvider;
import cn.forever24.tutor.ai.fake.FakeTextToSpeechProvider;
import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import cn.forever24.tutor.ai.provider.AsrOptions;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.VoiceOptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentModuleTest {

    @Test
    void fakeChatProviderReturnsDeterministicStructuredResponse() {
        FakeChatProvider provider = new FakeChatProvider();

        var response = provider.completeStructured(
                ChatProviderRequest.structured("trace-1", "prompt-v1", "schema-v1", "Coach me"),
                coachResponseSchema()
        );

        assertTrue(response.content().contains("\"reply\":\"This is a deterministic fake coach response.\""));
        assertTrue(response.content().contains("\"shouldContinue\":true"));
        assertEquals("trace-1", response.trace().traceId());
        assertEquals("prompt-v1", response.trace().promptVersion());
        assertEquals("schema-v1", response.trace().schemaVersion());
        assertEquals(FakeProviderVersions.CHAT_MODEL_ID, response.trace().modelId());
        assertEquals(0, response.usage().estimatedCostUsd().signum());
        assertFalse(response.repaired());
        assertTrue(provider.capabilities().capabilities().contains(ProviderCapability.STRUCTURED_OUTPUT));
    }

    @Test
    void fakeChatProviderReturnsDeterministicStreamChunks() {
        FakeChatProvider provider = new FakeChatProvider();

        var stream = provider.stream(
                ChatProviderRequest.structured("trace-2", "prompt-v1", "schema-v1", "Say hello")
        );

        assertEquals("This is a deterministic fake coach response.", String.join("", stream.chunks()));
        assertEquals("trace-2", stream.trace().traceId());
        assertTrue(provider.capabilities().capabilities().contains(ProviderCapability.CHAT_STREAMING));
    }

    @Test
    void fakeSpeechToTextProviderReturnsDeterministicTranscript() {
        FakeSpeechToTextProvider provider = new FakeSpeechToTextProvider();

        var result = provider.transcribe(
                new AudioInput("trace-asr", new byte[]{1, 2, 3}, "audio/wav", Duration.ofSeconds(2)),
                AsrOptions.english()
        );

        assertEquals("This is a deterministic fake transcription for local development.", result.transcript());
        assertEquals(0.97, result.confidence());
        assertEquals(FakeProviderVersions.ASR_MODEL_ID, result.trace().modelId());
        assertEquals(2_000, result.usage().audioInputMillis());
    }

    @Test
    void fakeTextToSpeechProviderReturnsDeterministicAudioBytes() {
        FakeTextToSpeechProvider provider = new FakeTextToSpeechProvider();

        var result = provider.synthesize("trace-tts", "hello", VoiceOptions.englishPcm());

        assertArrayEquals("FAKE_WAV:hello".getBytes(StandardCharsets.UTF_8), result.audio());
        assertEquals("audio/wav", result.contentType());
        assertEquals(FakeProviderVersions.TTS_MODEL_ID, result.trace().modelId());
        assertEquals(300, result.duration().toMillis());
    }

    @Test
    void blankChatInputFailsWithValidationError() {
        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> ChatProviderRequest.structured("trace", "prompt-v1", "schema-v1", " ")
        );

        assertEquals(AiProviderErrorType.VALIDATION_ERROR, exception.errorType());
    }

    @Test
    void emptyAudioInputFailsWithValidationError() {
        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> new AudioInput("trace", new byte[0], "audio/wav", Duration.ofSeconds(1))
        );

        assertEquals(AiProviderErrorType.VALIDATION_ERROR, exception.errorType());
    }

    @Test
    void blankSpeechTextFailsWithValidationError() {
        FakeTextToSpeechProvider provider = new FakeTextToSpeechProvider();

        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> provider.synthesize("trace", " ", VoiceOptions.englishPcm())
        );

        assertEquals(AiProviderErrorType.VALIDATION_ERROR, exception.errorType());
    }

    @Test
    void nullAudioInputFailsWithValidationError() {
        FakeSpeechToTextProvider provider = new FakeSpeechToTextProvider();

        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> provider.transcribe(null, AsrOptions.english())
        );

        assertEquals(AiProviderErrorType.VALIDATION_ERROR, exception.errorType());
    }

    private JsonSchema coachResponseSchema() {
        return new JsonSchema(
                "coach-response",
                "schema-v1",
                Map.of("type", "object")
        );
    }
}
