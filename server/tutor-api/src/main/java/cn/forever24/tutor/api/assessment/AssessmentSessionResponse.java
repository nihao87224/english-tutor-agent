package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.assessment.AssessmentSession;

public record AssessmentSessionResponse(
        String assessmentId,
        String status,
        int targetMinutes,
        Integer estimatedRemainingMinutes
) {

    public static AssessmentSessionResponse from(AssessmentSession session) {
        return new AssessmentSessionResponse(
                session.assessmentId(),
                session.status().name(),
                session.targetMinutes(),
                session.estimatedRemainingMinutes());
    }
}
