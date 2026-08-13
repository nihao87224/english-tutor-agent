package cn.forever24.tutor.assessment;

public record AssessmentAnswerReceipt(
        String answerId,
        boolean accepted
) {

    public AssessmentAnswerReceipt {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answer id is required");
        }
    }
}
