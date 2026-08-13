package cn.forever24.tutor.application.assessment;

public record AssessmentCompletion(
        String assessmentId,
        String status
) {

    public AssessmentCompletion {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessmentId is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
