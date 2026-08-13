package cn.forever24.tutor.application.training;

import cn.forever24.tutor.reporting.DailyTrainingSummary;
import cn.forever24.tutor.training.TrainingSession;

public record TrainingSessionCompletion(
        TrainingSession session,
        DailyTrainingSummary dailySummary
) {

    public TrainingSessionCompletion {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        if (dailySummary == null) {
            throw new IllegalArgumentException("daily summary is required");
        }
    }
}
