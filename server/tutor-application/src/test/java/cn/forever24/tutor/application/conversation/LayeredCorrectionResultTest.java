package cn.forever24.tutor.application.conversation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LayeredCorrectionResultTest {

    @Test
    void rejectsInvalidCorrectionShapes() {
        LayeredCorrectionItem lowInterrupting = new LayeredCorrectionItem(
                "maybe because",
                "may be because",
                "part_of_speech",
                CorrectionSeverity.LOW,
                "Low severity fixes should not interrupt.",
                false,
                false,
                List.of());

        assertThrows(IllegalArgumentException.class, () -> new LayeredCorrectionResult(
                true,
                List.of(),
                "Feedback.",
                "prompt-v1",
                "schema-v1",
                "trace-1",
                "openai",
                "test-chat-model"));
        assertThrows(IllegalArgumentException.class, () -> new LayeredCorrectionResult(
                false,
                List.of(lowInterrupting),
                "Feedback.",
                "prompt-v1",
                "schema-v1",
                "trace-1",
                "openai",
                "test-chat-model"));
        assertThrows(IllegalArgumentException.class, () -> new LayeredCorrectionItem(
                "maybe because",
                "may be because",
                "part_of_speech",
                CorrectionSeverity.LOW,
                "Low severity fixes should not interrupt.",
                true,
                false,
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new LayeredCorrectionResult(
                true,
                List.of(lowInterrupting, lowInterrupting, lowInterrupting, lowInterrupting),
                "Feedback.",
                "prompt-v1",
                "schema-v1",
                "trace-1",
                "openai",
                "test-chat-model"));
    }
}
