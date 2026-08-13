package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.application.assessment.AssessmentCompletion;

public record AssessmentCompletionResponse(
        String assessmentId,
        String status
) {

    public static AssessmentCompletionResponse from(AssessmentCompletion completion) {
        return new AssessmentCompletionResponse(completion.assessmentId(), completion.status());
    }
}
