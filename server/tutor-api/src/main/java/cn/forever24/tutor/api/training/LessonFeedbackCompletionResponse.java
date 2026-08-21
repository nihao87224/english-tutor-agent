package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.EvidenceSummary;

import java.util.List;

public record LessonFeedbackCompletionResponse(
        String attemptId, int evidenceCount, List<String> affectedSkills, String nextFocus
) {
    static LessonFeedbackCompletionResponse from(EvidenceSummary summary) {
        return new LessonFeedbackCompletionResponse(summary.attemptId(), summary.evidenceCount(),
                summary.affectedSkills(), summary.nextFocus());
    }
}
