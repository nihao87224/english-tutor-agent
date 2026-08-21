package cn.forever24.tutor.training;

/** A single, rubric-bound result. The criterion key always comes from the locked lesson. */
public record AttemptCriterionResult(String criterionKey, boolean satisfied, String feedback) {
    public AttemptCriterionResult {
        if (criterionKey == null || criterionKey.isBlank()) {
            throw new IllegalArgumentException("criterionKey is required");
        }
        criterionKey = criterionKey.strip();
        if (feedback == null || feedback.isBlank() || feedback.strip().length() > 500) {
            throw new IllegalArgumentException("criterion feedback is required and must be at most 500 characters");
        }
        feedback = feedback.strip();
    }
}
