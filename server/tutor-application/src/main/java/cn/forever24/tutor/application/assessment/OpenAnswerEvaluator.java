package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.OpenAnswerEvaluation;

public interface OpenAnswerEvaluator {

    String promptVersion();

    String schemaVersion();

    OpenAnswerEvaluation evaluate(OpenAnswerEvaluationRequest request);
}
