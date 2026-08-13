package cn.forever24.tutor.assessment;

public enum SelfRating {
    BEGINNER(1),
    BASIC(2),
    INTERMEDIATE(3),
    UPPER_INTERMEDIATE(4),
    ADVANCED(5);

    private final int score;

    SelfRating(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }

    public static SelfRating fromContractValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " rating is required");
        }
        try {
            return SelfRating.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported " + fieldName + " rating: " + value, exception);
        }
    }
}
