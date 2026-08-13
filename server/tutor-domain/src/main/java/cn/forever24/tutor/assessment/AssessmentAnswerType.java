package cn.forever24.tutor.assessment;

public enum AssessmentAnswerType {
    OPTION,
    TEXT,
    AUDIO;

    public static AssessmentAnswerType fromContractValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("answerType is required");
        }
        try {
            return AssessmentAnswerType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported answerType: " + value);
        }
    }
}
