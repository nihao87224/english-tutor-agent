package cn.forever24.tutor.assessment;

public record ObjectiveAssessmentItem(
        String itemId,
        String questionType,
        String correctOption
) {

    public ObjectiveAssessmentItem {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("item id is required");
        }
        if (questionType == null || questionType.isBlank()) {
            throw new IllegalArgumentException("question type is required");
        }
        if (correctOption == null || correctOption.isBlank()) {
            throw new IllegalArgumentException("correct option is required");
        }
    }

    public ObjectiveAnswerScore score(String submittedOption) {
        if (submittedOption == null || submittedOption.isBlank()) {
            throw new IllegalArgumentException("option is required for objective answers");
        }
        if (correctOption.equals(submittedOption.trim())) {
            return new ObjectiveAnswerScore(
                    AssessmentCorrectness.CORRECT,
                    ObjectiveAnswerScore.FULL_SCORE,
                    ObjectiveAnswerScore.DETERMINISTIC_CONFIDENCE);
        }
        return new ObjectiveAnswerScore(
                AssessmentCorrectness.INCORRECT,
                ObjectiveAnswerScore.ZERO_SCORE,
                ObjectiveAnswerScore.DETERMINISTIC_CONFIDENCE);
    }
}
