package cn.forever24.tutor.planning.policy;

import cn.forever24.tutor.curriculum.CompletionPolicy;

import java.util.Set;

public final class TaskCompletionPolicy {

    public Decision evaluate(CompletionPolicy policy, Progress progress) {
        if (policy == null || progress == null) {
            throw new IllegalArgumentException("completion policy and progress are required");
        }
        if (!progress.primaryInputCompleted()) {
            return decision(false, PolicyReasonCode.INPUT_INCOMPLETE);
        }
        if (!progress.comprehensionCompleted()) {
            return decision(false, PolicyReasonCode.COMPREHENSION_INCOMPLETE);
        }
        if (progress.completedOutputTasks() < policy.minimumOutputTasks()) {
            return decision(false, PolicyReasonCode.OUTPUT_REQUIRED);
        }
        if (!progress.passedCriterionKeys().containsAll(policy.requiredCriterionKeys())) {
            return decision(false, PolicyReasonCode.REQUIRED_CRITERION_NOT_MET);
        }
        return decision(true, PolicyReasonCode.COMPLETED_WITHOUT_MASTERY);
    }

    private static Decision decision(boolean completed, PolicyReasonCode reasonCode) {
        return new Decision(completed, false, reasonCode, PedagogicalPolicyVersion.V2_P0_1);
    }

    public record Progress(
            boolean primaryInputCompleted,
            boolean comprehensionCompleted,
            int completedOutputTasks,
            Set<String> passedCriterionKeys
    ) {
        public Progress {
            if (completedOutputTasks < 0 || passedCriterionKeys == null) {
                throw new IllegalArgumentException("valid completion progress is required");
            }
            passedCriterionKeys = Set.copyOf(passedCriterionKeys);
        }
    }

    public record Decision(
            boolean completed,
            boolean masteryChanged,
            PolicyReasonCode reasonCode,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("completion decision fields are required");
            }
            if (masteryChanged) {
                throw new IllegalArgumentException("completion must not change mastery");
            }
        }
    }
}
