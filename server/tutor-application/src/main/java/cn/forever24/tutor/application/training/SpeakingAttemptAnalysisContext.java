package cn.forever24.tutor.application.training;

import java.util.List;

/** Locked, server-owned prompt boundary for one speaking attempt. */
public record SpeakingAttemptAnalysisContext(
        String attemptId,
        String sessionId,
        String resourceId,
        String resourceVersion,
        String taskId,
        String prompt,
        List<String> criterionKeys,
        List<String> criteria,
        String learnerText
) {
    public SpeakingAttemptAnalysisContext {
        attemptId = required(attemptId, "attemptId");
        sessionId = required(sessionId, "sessionId");
        resourceId = required(resourceId, "resourceId");
        resourceVersion = required(resourceVersion, "resourceVersion");
        taskId = required(taskId, "taskId");
        prompt = required(prompt, "prompt");
        learnerText = required(learnerText, "learnerText");
        criterionKeys = List.copyOf(criterionKeys == null ? List.of() : criterionKeys);
        criteria = List.copyOf(criteria == null ? List.of() : criteria);
        if (criterionKeys.isEmpty() || criterionKeys.size() != criteria.size()
                || criterionKeys.stream().anyMatch(value -> value == null || value.isBlank())
                || criteria.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("locked criteria are required");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
