package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.fake.FakeChatProvider;
import cn.forever24.tutor.application.conversation.CorrectionAnalysisContext;
import cn.forever24.tutor.application.conversation.LayeredCorrectionResult;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderLayeredCorrectionAnalyzerTest {

    private final ProviderLayeredCorrectionAnalyzer analyzer =
            new ProviderLayeredCorrectionAnalyzer(new FakeChatProvider());

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
}
