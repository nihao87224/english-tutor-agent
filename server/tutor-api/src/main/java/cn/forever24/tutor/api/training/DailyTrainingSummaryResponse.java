package cn.forever24.tutor.api.training;

import cn.forever24.tutor.reporting.DailyTrainingSummary;

import java.time.Instant;
import java.util.List;

public record DailyTrainingSummaryResponse(
        String sessionId,
        int completedTaskCount,
        int evidenceCount,
        List<String> practicedSkills,
        List<String> highlights,
        List<String> memorableItems,
        List<String> nextFocus,
        Instant generatedAt
) {

    static DailyTrainingSummaryResponse from(DailyTrainingSummary summary) {
        return new DailyTrainingSummaryResponse(
                summary.sessionId(),
                summary.completedTaskCount(),
                summary.evidenceCount(),
                summary.practicedSkills(),
                summary.highlights(),
                summary.memorableItems(),
                summary.nextFocus(),
                summary.generatedAt());
    }
}
