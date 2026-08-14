package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.conversation.CorrectionAnalysisContext;
import cn.forever24.tutor.application.conversation.LayeredCorrectionResult;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderLayeredCorrectionAnalyzerTest {

    private final ProviderLayeredCorrectionAnalyzer analyzer =
            new ProviderLayeredCorrectionAnalyzer(new StubChatProvider());

    @Test
    void returnsNoErrorFeedbackForNaturalMessage() {
        LayeredCorrectionResult result = analyzer.analyze(new CorrectionAnalysisContext(
                session(),
                task(),
                "Today I fixed a database connection issue."));

        assertFalse(result.hasError());
        assertEquals(0, result.corrections().size());
        assertEquals("correction-analyzer-v1", result.promptVersion());
        assertEquals("correction-result-v1", result.schemaVersion());
    }

    @Test
    void returnsLayeredCorrectionsAndCapsCount() {
        LayeredCorrectionResult result = analyzer.analyze(new CorrectionAnalysisContext(
                session(),
                task(),
                "I very like it. He go there maybe because the meeting changed."));

        assertTrue(result.hasError());
        assertEquals(3, result.corrections().size());
        assertEquals("word_order", result.corrections().get(0).errorType());
        assertTrue(result.corrections().get(1).shouldInterrupt());
        assertFalse(result.corrections().get(2).shouldInterrupt());
    }

    private TrainingSession session() {
        return TrainingSession.startDaily(
                "training-1",
                "plan-1",
                TrainingSessionMode.TEXT,
                "task-1",
                Instant.parse("2026-08-10T08:00:00Z"));
    }

    private LearningPlanTask task() {
        return new LearningPlanTask(
                "task-1",
                "CONVERSATION",
                "Ask a follow-up question",
                10,
                List.of("speaking"),
                "A2",
                "Practice natural follow-up questions.");
    }

    private static final class StubChatProvider implements ChatProvider {
        @Override
        public ChatStream stream(ChatProviderRequest request) {
            return new ChatStream(List.of("ok"), trace(request), ProviderUsage.freeText(1, 1));
        }

        @Override
        public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
            return new StructuredResponse("{}", trace(request), ProviderUsage.freeText(10, 1), false);
        }

        @Override
        public ProviderCapabilities capabilities() {
            return new ProviderCapabilities("test-openai", "test-model", Set.of(ProviderCapability.STRUCTURED_OUTPUT), java.time.Duration.ofSeconds(1));
        }

        private ProviderTrace trace(ChatProviderRequest request) {
            return new ProviderTrace(request.traceId(), "test-openai", "test-model", request.promptVersion(), request.schemaVersion());
        }
    }
}
