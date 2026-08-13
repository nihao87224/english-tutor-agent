package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;

import java.time.LocalDate;
import java.util.List;

public record LearningPlanResponse(
        String planId,
        LocalDate date,
        int totalMinutes,
        List<PlanTaskResponse> tasks,
        List<String> reasons,
        boolean temporaryAdjustment
) {

    public static LearningPlanResponse from(LearningPlan plan) {
        return new LearningPlanResponse(
                plan.planId(),
                plan.date(),
                plan.totalMinutes(),
                plan.tasks().stream().map(PlanTaskResponse::from).toList(),
                plan.reasons(),
                plan.temporaryAdjustment());
    }

    public record PlanTaskResponse(
            String taskId,
            String type,
            String title,
            int durationMinutes,
            List<String> skillFocus,
            String difficulty,
            String reason
    ) {

        private static PlanTaskResponse from(LearningPlanTask task) {
            return new PlanTaskResponse(
                    task.taskId(),
                    task.type(),
                    task.title(),
                    task.durationMinutes(),
                    task.skillFocus(),
                    task.difficulty(),
                    task.reason());
        }
    }
}
