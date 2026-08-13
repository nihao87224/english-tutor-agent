package cn.forever24.tutor.assessment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAnswerEvaluationTest {

    @Test
    void acceptsValidOpenEvaluation() {
        OpenAnswerEvaluation evaluation = new OpenAnswerEvaluation(
                AssessmentCorrectness.PARTIAL,
                new BigDecimal("0.5600"),
                new BigDecimal("0.8000"),
                "Understandable response.",
                "open-answer-evaluator-v1",
                "open-answer-evaluation-v1");

        assertEquals(AssessmentCorrectness.PARTIAL, evaluation.correctness());
    }

    @Test
    void rejectsOutOfRangeScore() {
        assertThrows(IllegalArgumentException.class, () -> new OpenAnswerEvaluation(
                AssessmentCorrectness.PARTIAL,
                new BigDecimal("1.2000"),
                new BigDecimal("0.8000"),
                "Understandable response.",
                "open-answer-evaluator-v1",
                "open-answer-evaluation-v1"));
    }

    @Test
    void rejectsBlankOpenAnswerText() {
        assertThrows(IllegalArgumentException.class, () -> OpenAssessmentItemBank.requireAnswerText(" "));
    }
}
