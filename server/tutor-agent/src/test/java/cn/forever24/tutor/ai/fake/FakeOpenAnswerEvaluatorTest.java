package cn.forever24.tutor.ai.fake;

import cn.forever24.tutor.application.assessment.OpenAnswerEvaluationRequest;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.OpenAnswerEvaluation;
import cn.forever24.tutor.assessment.OpenAssessmentItemBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeOpenAnswerEvaluatorTest {

    private final FakeOpenAnswerEvaluator evaluator = new FakeOpenAnswerEvaluator();

    @Test
    void evaluatesReasonedOpenAnswerDeterministically() {
        OpenAnswerEvaluation evaluation = evaluator.evaluate(new OpenAnswerEvaluationRequest(
                OpenAssessmentItemBank.requireOpenTextItem("initial-speaking-open-1"),
                "It was delayed because the team needed more time."));

        assertEquals(AssessmentCorrectness.CORRECT, evaluation.correctness());
        assertEquals("0.7600", evaluation.score().toPlainString());
        assertEquals("open-answer-evaluator-v1", evaluation.promptVersion());
        assertEquals("open-answer-evaluation-v1", evaluation.schemaVersion());
    }
}
