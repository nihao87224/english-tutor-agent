package cn.forever24.tutor.training;

public record LessonObjectiveResult(
        boolean correct,
        String expectedAnswer,
        String explanation
) {
    public LessonObjectiveResult {
        if (expectedAnswer == null || expectedAnswer.isBlank()) {
            throw new IllegalArgumentException("expectedAnswer is required");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation is required");
        }
    }
}
