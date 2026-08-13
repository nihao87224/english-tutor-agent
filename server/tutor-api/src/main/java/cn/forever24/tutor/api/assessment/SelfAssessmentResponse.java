package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.assessment.SelfAssessmentResult;

public record SelfAssessmentResponse(
        String selfAssessmentId,
        String estimatedBand
) {

    public static SelfAssessmentResponse from(SelfAssessmentResult result) {
        return new SelfAssessmentResponse(
                result.selfAssessmentId(),
                result.estimatedBand().name());
    }
}
