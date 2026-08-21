package cn.forever24.tutor.application.roleplay;

import java.util.List;

public record RolePlayTask(
        String taskId,
        String goal,
        String learnerRole,
        String aiRole,
        List<String> successCriteria,
        String openingLine
) {
    public RolePlayTask {
        taskId = required(taskId, "taskId");
        goal = required(goal, "goal");
        learnerRole = required(learnerRole, "learnerRole");
        aiRole = required(aiRole, "aiRole");
        successCriteria = List.copyOf(successCriteria == null ? List.of() : successCriteria);
        if (successCriteria.isEmpty()) throw new IllegalArgumentException("successCriteria is required");
        openingLine = required(openingLine, "openingLine");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
