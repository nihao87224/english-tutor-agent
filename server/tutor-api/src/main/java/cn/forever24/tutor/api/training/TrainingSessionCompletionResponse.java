package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.TrainingSessionCompletion;

public record TrainingSessionCompletionResponse(
        TrainingSessionResponse session,
        DailyTrainingSummaryResponse dailySummary
) {

    static TrainingSessionCompletionResponse from(TrainingSessionCompletion completion) {
        return new TrainingSessionCompletionResponse(
                TrainingSessionResponse.from(completion.session()),
                DailyTrainingSummaryResponse.from(completion.dailySummary()));
    }
}
