package cn.forever24.tutor.assessment;

public record SelfAssessmentResult(
        String selfAssessmentId,
        SelfRating estimatedBand
) {

    public SelfAssessmentResult {
        if (selfAssessmentId == null || selfAssessmentId.isBlank()) {
            throw new IllegalArgumentException("self assessment id is required");
        }
        if (estimatedBand == null) {
            throw new IllegalArgumentException("estimated band is required");
        }
    }
}
