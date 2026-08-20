package cn.forever24.tutor.planning.policy;

public final class AttemptRetryPolicy {

    public Decision evaluate(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("retry input is required");
        }
        if (input.failedCriteria() == 0) {
            return decision(Action.ACCEPT, PolicyReasonCode.ATTEMPT_ACCEPTED);
        }
        if (!input.criticalFailure()) {
            return decision(Action.ACCEPT, PolicyReasonCode.ATTEMPT_ACCEPTED);
        }
        if (input.attemptNumber() >= input.maximumAttempts()) {
            return decision(Action.FINAL_FAILURE, PolicyReasonCode.RETRY_LIMIT_REACHED);
        }
        return decision(Action.RETRY, PolicyReasonCode.RETRY_REQUIRED);
    }

    private static Decision decision(Action action, PolicyReasonCode reasonCode) {
        return new Decision(action, reasonCode, PedagogicalPolicyVersion.V2_P0_1);
    }

    public enum Action {
        ACCEPT,
        RETRY,
        FINAL_FAILURE
    }

    public record Input(boolean criticalFailure, int failedCriteria, int attemptNumber, int maximumAttempts) {
        public Input {
            if (failedCriteria < 0) {
                throw new IllegalArgumentException("failedCriteria must not be negative");
            }
            if (attemptNumber < 1 || maximumAttempts < 1 || maximumAttempts > 5
                    || attemptNumber > maximumAttempts) {
                throw new IllegalArgumentException("valid attempt bounds are required");
            }
        }
    }

    public record Decision(Action action, PolicyReasonCode reasonCode, PedagogicalPolicyVersion policyVersion) {
        public Decision {
            if (action == null || reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("retry decision fields are required");
            }
        }
    }
}
