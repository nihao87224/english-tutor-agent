package cn.forever24.tutor.training;

import java.time.Instant;

public record TrainingSession(
        String sessionId,
        String planId,
        TrainingSessionType type,
        TrainingSessionMode mode,
        TrainingSessionStatus status,
        String currentTaskId,
        Instant startedAt,
        Instant pausedAt,
        Instant completedAt,
        int effectiveSeconds,
        long version
) {

    public TrainingSession {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (currentTaskId == null || currentTaskId.isBlank()) {
            throw new IllegalArgumentException("currentTaskId is required");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt is required");
        }
        if (effectiveSeconds < 0) {
            throw new IllegalArgumentException("effectiveSeconds must not be negative");
        }
    }

    public static TrainingSession startDaily(
            String sessionId,
            String planId,
            TrainingSessionMode mode,
            String firstTaskId,
            Instant now
    ) {
        return new TrainingSession(
                sessionId,
                planId,
                TrainingSessionType.DAILY,
                mode == null ? TrainingSessionMode.MIXED : mode,
                TrainingSessionStatus.IN_PROGRESS,
                firstTaskId,
                now,
                null,
                null,
                0,
                0);
    }

    public TrainingSession pause(Instant now) {
        if (!status.canPause()) {
            throw new IllegalStateException("training session cannot be paused from " + status);
        }
        return new TrainingSession(
                sessionId,
                planId,
                type,
                mode,
                TrainingSessionStatus.PAUSED,
                currentTaskId,
                startedAt,
                now,
                null,
                effectiveSeconds,
                version + 1);
    }

    public TrainingSession resume() {
        if (!status.canResume()) {
            throw new IllegalStateException("training session cannot be resumed from " + status);
        }
        return new TrainingSession(
                sessionId,
                planId,
                type,
                mode,
                TrainingSessionStatus.IN_PROGRESS,
                currentTaskId,
                startedAt,
                null,
                null,
                effectiveSeconds,
                version + 1);
    }

    public TrainingSession complete(Instant now) {
        if (!status.canComplete()) {
            throw new IllegalStateException("training session cannot be completed from " + status);
        }
        return new TrainingSession(
                sessionId,
                planId,
                type,
                mode,
                TrainingSessionStatus.COMPLETED,
                currentTaskId,
                startedAt,
                pausedAt,
                now,
                effectiveSeconds,
                version + 1);
    }

    public TrainingSession moveToTask(String nextTaskId) {
        if (nextTaskId == null || nextTaskId.isBlank()) {
            throw new IllegalArgumentException("nextTaskId is required");
        }
        if (status != TrainingSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("training session must be IN_PROGRESS to move tasks");
        }
        return new TrainingSession(
                sessionId,
                planId,
                type,
                mode,
                status,
                nextTaskId,
                startedAt,
                pausedAt,
                completedAt,
                effectiveSeconds,
                version + 1);
    }
}
