package cn.forever24.tutor.api.assessment;

public record AssessmentAnswerRequest(
        String itemId,
        String answerType,
        String option,
        String text,
        String audioAssetId,
        Integer clientDurationMs
) {
}
