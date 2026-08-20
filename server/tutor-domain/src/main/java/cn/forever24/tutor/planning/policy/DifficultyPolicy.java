package cn.forever24.tutor.planning.policy;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;

public final class DifficultyPolicy {

    public Decision evaluate(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("difficulty input is required");
        }
        if (input.consecutiveFailures() >= 2) {
            return decision(input.currentLevel(), 2, ScaffoldingLevel.HIGH,
                    PolicyReasonCode.DIFFICULTY_REDUCED_AFTER_FAILURES);
        }
        if (input.consecutiveEasyCompletions() >= 3) {
            return decision(nextLevel(input.currentLevel()), 4, ScaffoldingLevel.LOW,
                    PolicyReasonCode.DIFFICULTY_INCREASED_AFTER_EASE);
        }
        return decision(input.currentLevel(), 3, ScaffoldingLevel.MEDIUM,
                PolicyReasonCode.DIFFICULTY_MAINTAINED);
    }

    private static Decision decision(
            CefrLevel level,
            int maximumComplexity,
            ScaffoldingLevel scaffolding,
            PolicyReasonCode reasonCode
    ) {
        return new Decision(level, maximumComplexity, scaffolding, reasonCode,
                PedagogicalPolicyVersion.V2_P0_1);
    }

    private static CefrLevel nextLevel(CefrLevel level) {
        CefrLevel[] levels = CefrLevel.values();
        return levels[Math.min(level.ordinal() + 1, levels.length - 1)];
    }

    public record Input(CefrLevel currentLevel, int consecutiveFailures, int consecutiveEasyCompletions) {
        public Input {
            if (currentLevel == null) {
                throw new IllegalArgumentException("currentLevel is required");
            }
            if (consecutiveFailures < 0 || consecutiveEasyCompletions < 0) {
                throw new IllegalArgumentException("difficulty streaks must not be negative");
            }
            if (consecutiveFailures > 0 && consecutiveEasyCompletions > 0) {
                throw new IllegalArgumentException("failure and ease streaks cannot both be active");
            }
        }
    }

    public record Decision(
            CefrLevel targetLevel,
            int maximumCommunicationComplexity,
            ScaffoldingLevel scaffolding,
            PolicyReasonCode reasonCode,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (targetLevel == null || scaffolding == null || reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("difficulty decision fields are required");
            }
            if (maximumCommunicationComplexity < 1 || maximumCommunicationComplexity > 5) {
                throw new IllegalArgumentException("maximumCommunicationComplexity must be between 1 and 5");
            }
        }
    }
}
