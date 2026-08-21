package cn.forever24.tutor.application.training;

import java.util.List;

public record GuidedSpeakingTask(String taskId, String prompt, List<String> successCriteria, List<String> scaffolding) {
    public GuidedSpeakingTask {
        if (taskId == null || taskId.isBlank() || prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("guided speaking taskId and prompt are required");
        }
        taskId = taskId.strip();
        prompt = prompt.strip();
        successCriteria = successCriteria == null ? List.of() : List.copyOf(successCriteria);
        scaffolding = scaffolding == null ? List.of() : List.copyOf(scaffolding);
    }
}
