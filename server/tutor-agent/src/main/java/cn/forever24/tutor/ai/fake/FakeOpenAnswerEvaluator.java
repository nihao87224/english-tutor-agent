package cn.forever24.tutor.ai.fake;

import cn.forever24.tutor.application.assessment.OpenAnswerEvaluationRequest;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.ObjectiveAnswerScore;
import cn.forever24.tutor.assessment.OpenAnswerEvaluation;

import java.math.BigDecimal;

public class FakeOpenAnswerEvaluator implements OpenAnswerEvaluator {

    public static final String PROMPT_VERSION = "open-answer-evaluator-v1";
    public static final String SCHEMA_VERSION = "open-answer-evaluation-v1";

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    @Override
    public String schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public OpenAnswerEvaluation evaluate(OpenAnswerEvaluationRequest request) {
        String text = request.text().toLowerCase();
        boolean hasReasoningMarker = text.contains("because") || text.contains("so ") || text.contains("therefore");
        BigDecimal score = hasReasoningMarker ? new BigDecimal("0.7600") : new BigDecimal("0.5600");
        AssessmentCorrectness correctness = score.compareTo(new BigDecimal("0.7000")) >= 0
                ? AssessmentCorrectness.CORRECT
                : AssessmentCorrectness.PARTIAL;
        String feedback = hasReasoningMarker
                ? "Clear response with a reason. Keep tightening grammar and word choice."
                : "Understandable response. Add a clear reason or example next time.";
        return new OpenAnswerEvaluation(
                correctness,
                score,
                ObjectiveAnswerScore.DETERMINISTIC_CONFIDENCE,
                feedback,
                PROMPT_VERSION,
                SCHEMA_VERSION);
    }
}
