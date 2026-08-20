package cn.forever24.tutor.planning.policy;

import cn.forever24.tutor.curriculum.Prerequisite;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PrerequisitePolicy {

    public Decision evaluate(List<Prerequisite> prerequisites, Map<String, SkillState> skillStates) {
        if (prerequisites == null || skillStates == null) {
            throw new IllegalArgumentException("prerequisites and skillStates are required");
        }
        List<String> unmet = prerequisites.stream()
                .filter(prerequisite -> !isMet(prerequisite, skillStates.get(prerequisite.skillKey())))
                .map(Prerequisite::skillKey)
                .sorted()
                .toList();
        return new Decision(
                unmet.isEmpty(),
                unmet.isEmpty() ? PolicyReasonCode.ELIGIBLE : PolicyReasonCode.PREREQUISITE_NOT_MET,
                unmet,
                PedagogicalPolicyVersion.V2_P0_1);
    }

    private static boolean isMet(Prerequisite prerequisite, SkillState state) {
        return state != null
                && state.mastery().compareTo(prerequisite.minimumMastery()) >= 0
                && state.confidence().compareTo(prerequisite.minimumConfidence()) >= 0;
    }

    public record SkillState(BigDecimal mastery, BigDecimal confidence) {
        public SkillState {
            mastery = ProbabilitySupport.require(mastery, "mastery");
            confidence = ProbabilitySupport.require(confidence, "confidence");
        }
    }

    public record Decision(
            boolean eligible,
            PolicyReasonCode reasonCode,
            List<String> unmetSkillKeys,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (reasonCode == null || policyVersion == null || unmetSkillKeys == null) {
                throw new IllegalArgumentException("prerequisite decision fields are required");
            }
            unmetSkillKeys = unmetSkillKeys.stream().sorted(Comparator.naturalOrder()).toList();
        }
    }
}
