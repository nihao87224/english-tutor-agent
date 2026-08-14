package cn.forever24.tutor.ai;

import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.VoiceOptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentModuleTest {

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
        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> cn.forever24.tutor.ai.provider.ProviderText.requireNonBlank(" ", "text")
        );

        assertEquals(AiProviderErrorType.VALIDATION_ERROR, exception.errorType());
    }

    @Test
    void defaultEnglishVoiceUsesProviderNeutralAlias() {
        assertEquals("english-neutral", VoiceOptions.englishPcm().voiceId());
    }
}
