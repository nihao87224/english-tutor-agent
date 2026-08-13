package cn.forever24.tutor.assessment;

public record ScoredObjectiveAnswer(
        String itemId,
        String questionType,
        String option,
        ObjectiveAnswerScore score,
        Integer clientDurationMs
) {

    public ScoredObjectiveAnswer {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        if (questionType == null || questionType.isBlank()) {
            throw new IllegalArgumentException("question type is required");
        }
        if (option == null || option.isBlank()) {
            throw new IllegalArgumentException("option is required");
        }
        if (score == null) {
            throw new IllegalArgumentException("score is required");
        }
        if (clientDurationMs != null && clientDurationMs < 0) {
            throw new IllegalArgumentException("clientDurationMs must not be negative");
        }
    }
}
