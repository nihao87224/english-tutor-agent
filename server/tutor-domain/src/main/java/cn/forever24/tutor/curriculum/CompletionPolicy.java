package cn.forever24.tutor.curriculum;

import java.util.Set;

public record CompletionPolicy(
        int minimumOutputTasks,
        Set<String> requiredCriterionKeys,
        boolean completionDoesNotImplyMastery
) {

    public CompletionPolicy {
        if (minimumOutputTasks < 1 || minimumOutputTasks > 10) {
            throw new IllegalArgumentException("minimumOutputTasks must be between 1 and 10");
        }
        if (requiredCriterionKeys == null || requiredCriterionKeys.isEmpty()) {
            throw new IllegalArgumentException("requiredCriterionKeys must not be empty");
        }
        requiredCriterionKeys = Set.copyOf(requiredCriterionKeys);
        if (!completionDoesNotImplyMastery) {
            throw new IllegalArgumentException("completion must not imply mastery");
        }
    }
}
