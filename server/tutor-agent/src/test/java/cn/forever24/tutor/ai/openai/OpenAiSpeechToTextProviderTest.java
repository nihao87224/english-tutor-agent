package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.AiProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiSpeechToTextProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void usesProviderConfidenceWhenPresent() throws Exception {
        assertEquals(0.87, OpenAiSpeechToTextProvider.confidence(
                objectMapper.readTree("{\"text\":\"Gate 24\",\"confidence\":0.87}")));
    }

    @Test
    void treatsMissingConfidenceAsUnconfirmedAndRejectsInvalidValues() throws Exception {
        assertEquals(0.0, OpenAiSpeechToTextProvider.confidence(
                objectMapper.readTree("{\"text\":\"Gate 24\"}")));
        assertThrows(AiProviderException.class, () -> OpenAiSpeechToTextProvider.confidence(
                objectMapper.readTree("{\"text\":\"Gate 24\",\"confidence\":1.2}")));
    }
}
