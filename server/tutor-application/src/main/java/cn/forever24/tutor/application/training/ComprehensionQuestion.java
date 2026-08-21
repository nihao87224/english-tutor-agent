package cn.forever24.tutor.application.training;

public record ComprehensionQuestion(String questionId, String prompt, String answer) {
    public ComprehensionQuestion {
        questionId = required(questionId, "questionId");
        prompt = required(prompt, "prompt");
        answer = required(answer, "answer");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
