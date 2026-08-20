package cn.forever24.tutor.planning.policy;

import java.util.Set;

public final class TransferPolicy {

    public Decision evaluate(boolean mastered, Set<String> practicedContextKeys, String candidateContextKey) {
        if (practicedContextKeys == null || candidateContextKey == null || candidateContextKey.isBlank()) {
            throw new IllegalArgumentException("practiced contexts and candidate context are required");
        }
        Set<String> contexts = Set.copyOf(practicedContextKeys);
        if (!mastered || contexts.isEmpty()) {
            return decision(false, PolicyReasonCode.TRANSFER_NOT_READY);
        }
        if (contexts.contains(candidateContextKey.strip())) {
            return decision(false, PolicyReasonCode.TRANSFER_CONTEXT_ALREADY_USED);
        }
        return decision(true, PolicyReasonCode.TRANSFER_TO_NEW_CONTEXT);
    }

    private static Decision decision(boolean transferCandidate, PolicyReasonCode reasonCode) {
        return new Decision(transferCandidate, reasonCode, PedagogicalPolicyVersion.V2_P0_1);
    }

    public record Decision(
            boolean transferCandidate,
            PolicyReasonCode reasonCode,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("transfer decision fields are required");
            }
        }
    }
}
