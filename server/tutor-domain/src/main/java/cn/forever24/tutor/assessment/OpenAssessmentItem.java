package cn.forever24.tutor.assessment;

public record OpenAssessmentItem(
        String itemId,
        String questionType
) {

    public OpenAssessmentItem {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("item id is required");
        }
        if (questionType == null || questionType.isBlank()) {
            throw new IllegalArgumentException("question type is required");
        }
    }
}
