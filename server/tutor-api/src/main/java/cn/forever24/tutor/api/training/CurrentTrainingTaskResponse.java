package cn.forever24.tutor.api.training;

import cn.forever24.tutor.training.CurrentTrainingTask;

import java.util.List;

public record CurrentTrainingTaskResponse(
        String taskId,
        String type,
        String title,
        int durationMinutes,
        List<String> skillFocus,
        String difficulty,
        String reason,
        String status
) {

    static CurrentTrainingTaskResponse from(CurrentTrainingTask currentTask) {
        return new CurrentTrainingTaskResponse(
                currentTask.task().taskId(),
                currentTask.task().type(),
                currentTask.task().title(),
                currentTask.task().durationMinutes(),
                currentTask.task().skillFocus(),
                currentTask.task().difficulty(),
                currentTask.task().reason(),
                currentTask.status().name());
    }
}
