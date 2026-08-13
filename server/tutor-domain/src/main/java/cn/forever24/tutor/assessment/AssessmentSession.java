package cn.forever24.tutor.assessment;

public record AssessmentSession(
        String assessmentId,
        AssessmentSessionStatus status,
        int targetMinutes,
        Integer estimatedRemainingMinutes
) {
    public static final int DEFAULT_TARGET_MINUTES = 9;
    public static final int MIN_TARGET_MINUTES = 5;
    public static final int MAX_TARGET_MINUTES = 15;

    public AssessmentSession {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessment id is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("assessment status is required");
        }
        validateTargetMinutes(targetMinutes);
        if (estimatedRemainingMinutes != null && estimatedRemainingMinutes < 0) {
            throw new IllegalArgumentException("estimated remaining minutes must not be negative");
        }
    }

    public static int resolveTargetMinutes(Integer targetMinutes) {
        int resolved = targetMinutes == null ? DEFAULT_TARGET_MINUTES : targetMinutes;
        validateTargetMinutes(resolved);
        return resolved;
    }

    private static void validateTargetMinutes(int targetMinutes) {
        if (targetMinutes < MIN_TARGET_MINUTES || targetMinutes > MAX_TARGET_MINUTES) {
            throw new IllegalArgumentException("targetMinutes must be between 5 and 15");
        }
    }
}
