package cn.forever24.tutor.training;

public enum TrainingSessionStatus {
    CREATED,
    IN_PROGRESS,
    PAUSED,
    COMPLETING,
    COMPLETED,
    COMPLETION_FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canPause() {
        return this == IN_PROGRESS;
    }

    public boolean canResume() {
        return this == PAUSED;
    }

    public boolean canComplete() {
        return this == IN_PROGRESS || this == PAUSED || this == COMPLETION_FAILED;
    }
}
