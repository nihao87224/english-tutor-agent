package cn.forever24.tutor.api.training;

import cn.forever24.tutor.training.TrainingSession;

import java.time.Instant;

public record TrainingSessionResponse(
        String sessionId,
        String planId,
        String type,
        String mode,
        String status,
        String currentTaskId,
        Instant startedAt,
        Instant pausedAt,
        Instant completedAt,
        int effectiveSeconds
) {

    static TrainingSessionResponse from(TrainingSession session) {
        return new TrainingSessionResponse(
                session.sessionId(),
                session.planId(),
                session.type().name(),
                session.mode().name(),
                session.status().name(),
                session.currentTaskId(),
                session.startedAt(),
                session.pausedAt(),
                session.completedAt(),
                session.effectiveSeconds());
    }
}
