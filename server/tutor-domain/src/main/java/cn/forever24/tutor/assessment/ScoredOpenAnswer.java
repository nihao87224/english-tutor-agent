package cn.forever24.tutor.assessment;

public record ScoredOpenAnswer(
        String itemId,
        String questionType,
        String text,
        OpenAnswerEvaluation evaluation,
        Integer clientDurationMs
) {

    public ScoredOpenAnswer {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        if (questionType == null || questionType.isBlank()) {
            throw new IllegalArgumentException("question type is required");
        }
        text = OpenAssessmentItemBank.requireAnswerText(text);
        if (evaluation == null) {
            throw new IllegalArgumentException("evaluation is required");
        }
        if (clientDurationMs != null && clientDurationMs < 0) {
            throw new IllegalArgumentException("clientDurationMs must not be negative");
        }
    }
}
