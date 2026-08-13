package cn.forever24.tutor.api.assessment;

public record SelfAssessmentRequest(
        String listening,
        String speaking,
        String reading,
        String writing
) {
}
