package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonStep;

import java.util.List;

public record LessonAttemptProgress(
        LessonStep step,
        List<String> completedTaskIds,
        List<String> remainingTaskIds,
        boolean nextStepEligible,
        String pendingAttemptId
) {
    public LessonAttemptProgress {
        completedTaskIds = List.copyOf(completedTaskIds);
        remainingTaskIds = List.copyOf(remainingTaskIds);
    }
}
