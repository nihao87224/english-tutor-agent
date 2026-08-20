package cn.forever24.tutor.planning.policy;

import java.math.BigDecimal;

public final class MasteryEligibilityPolicy {

    public static final BigDecimal MASTERED_THRESHOLD = new BigDecimal("0.8000");

    public Decision evaluate(
            BigDecimal mastery,
            boolean reviewDue,
            boolean higherDifficulty,
            boolean newContextTransfer
    ) {
        BigDecimal normalizedMastery = ProbabilitySupport.require(mastery, "mastery");
        if (normalizedMastery.compareTo(MASTERED_THRESHOLD) < 0) {
            return decision(true, LearningRoute.ACQUISITION, PolicyReasonCode.ACQUISITION_REQUIRED);
        }
        if (reviewDue) {
            return decision(true, LearningRoute.REVIEW, PolicyReasonCode.MASTERED_REVIEW_DUE);
        }
        if (higherDifficulty) {
            return decision(true, LearningRoute.UPGRADE, PolicyReasonCode.MASTERED_DIFFICULTY_UPGRADE);
        }
        if (newContextTransfer) {
            return decision(true, LearningRoute.TRANSFER, PolicyReasonCode.MASTERED_TRANSFER_REQUIRED);
        }
        return decision(false, LearningRoute.NONE, PolicyReasonCode.MASTERED_BASE_ACQUISITION_BLOCKED);
    }

    private static Decision decision(boolean eligible, LearningRoute route, PolicyReasonCode reasonCode) {
        return new Decision(eligible, route, reasonCode, PedagogicalPolicyVersion.V2_P0_1);
    }

    public enum LearningRoute {
        ACQUISITION,
        REVIEW,
        UPGRADE,
        TRANSFER,
        NONE
    }

    public record Decision(
            boolean eligible,
            LearningRoute route,
            PolicyReasonCode reasonCode,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (route == null || reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("mastery eligibility decision fields are required");
            }
        }
    }
}
