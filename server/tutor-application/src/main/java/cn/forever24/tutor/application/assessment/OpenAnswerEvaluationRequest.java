package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.OpenAssessmentItem;

public record OpenAnswerEvaluationRequest(
        OpenAssessmentItem item,
        String text
) {

    public OpenAnswerEvaluationRequest {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
    }
}
