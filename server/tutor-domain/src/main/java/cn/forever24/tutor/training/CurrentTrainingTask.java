package cn.forever24.tutor.training;

import cn.forever24.tutor.planning.LearningPlanTask;

public record CurrentTrainingTask(
        LearningPlanTask task,
        TrainingTaskStatus status
) {

    public CurrentTrainingTask {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
