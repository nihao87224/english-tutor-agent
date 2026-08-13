package cn.forever24.tutor.planning;

import java.util.List;

public record LearningPlanTask(
        String taskId,
        String type,
        String title,
        int durationMinutes,
        List<String> skillFocus,
        String difficulty,
        String reason
) {

    public LearningPlanTask {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        skillFocus = List.copyOf(skillFocus == null ? List.of() : skillFocus);
        if (skillFocus.isEmpty() || skillFocus.size() > 3) {
            throw new IllegalArgumentException("skillFocus must contain 1-3 items");
        }
        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException("difficulty is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
    }
}
