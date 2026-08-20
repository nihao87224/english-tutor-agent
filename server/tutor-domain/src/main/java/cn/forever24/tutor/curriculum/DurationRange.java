package cn.forever24.tutor.curriculum;

public record DurationRange(int minimumMinutes, int maximumMinutes) {

    public DurationRange {
        if (minimumMinutes < 1 || maximumMinutes < minimumMinutes || maximumMinutes > 120) {
            throw new IllegalArgumentException("duration range must be ordered and between 1 and 120 minutes");
        }
    }
}
