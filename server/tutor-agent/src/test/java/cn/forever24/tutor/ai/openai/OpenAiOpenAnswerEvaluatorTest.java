package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluationRequest;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.OpenAssessmentItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiOpenAnswerEvaluatorTest {

    @Test
    void mapsStructuredProviderJsonToOpenAnswerEvaluation() {
        OpenAiOpenAnswerEvaluator evaluator = new OpenAiOpenAnswerEvaluator(new StubChatProvider(), new ObjectMapper());

        var result = evaluator.evaluate(new OpenAnswerEvaluationRequest(
                new OpenAssessmentItem("item-1", "SHORT_TEXT"),
                "I agree because it helps people practice."));

        assertEquals(AssessmentCorrectness.CORRECT, result.correctness());
        assertEquals("0.82", result.score().stripTrailingZeros().toPlainString());
        assertEquals(OpenAiOpenAnswerEvaluator.PROMPT_VERSION, result.promptVersion());
    }

    private static final class StubChatProvider implements ChatProvider {
        @Override
        public ChatStream stream(ChatProviderRequest request) {
            return new ChatStream(List.of("ok"), trace(request), ProviderUsage.freeText(1, 1));
        }

        @Override
        public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
            return new StructuredResponse(
                    "{\"correctness\":\"CORRECT\",\"score\":0.82,\"confidence\":0.78,\"feedback\":\"Clear answer with useful reasoning.\"}",
                    trace(request),
                    ProviderUsage.freeText(20, 20),
                    false);
        }

        @Override
        public ProviderCapabilities capabilities() {
            return new ProviderCapabilities("openai", "test-model", Set.of(ProviderCapability.STRUCTURED_OUTPUT), Duration.ofSeconds(1));
        }

        private ProviderTrace trace(ChatProviderRequest request) {
            return new ProviderTrace(request.traceId(), "openai", "test-model", request.promptVersion(), request.schemaVersion());
        }
    }
}
