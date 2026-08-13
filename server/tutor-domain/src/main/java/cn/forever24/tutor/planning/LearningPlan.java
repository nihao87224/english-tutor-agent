package cn.forever24.tutor.planning;

import java.time.LocalDate;
import java.util.List;

public record LearningPlan(
        String planId,
        LocalDate date,
        int totalMinutes,
        List<LearningPlanTask> tasks,
        List<String> reasons,
        boolean temporaryAdjustment,
        long profileVersion
) {

    public LearningPlan {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (totalMinutes <= 0) {
            throw new IllegalArgumentException("totalMinutes must be positive");
        }
        tasks = List.copyOf(tasks == null ? List.of() : tasks);
        if (tasks.isEmpty() || tasks.size() > 3) {
            throw new IllegalArgumentException("plan must contain 1-3 tasks");
        }
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
        if (reasons.isEmpty() || reasons.size() > 3) {
            throw new IllegalArgumentException("plan must contain 1-3 reasons");
        }
        int taskMinutes = tasks.stream().mapToInt(LearningPlanTask::durationMinutes).sum();
        if (taskMinutes != totalMinutes) {
            throw new IllegalArgumentException("totalMinutes must equal task minutes");
        }
        if (profileVersion <= 0) {
            throw new IllegalArgumentException("profileVersion must be positive");
        }
    }
}
