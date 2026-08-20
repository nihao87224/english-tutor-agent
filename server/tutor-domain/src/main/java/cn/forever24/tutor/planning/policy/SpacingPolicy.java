package cn.forever24.tutor.planning.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class SpacingPolicy {

    private static final int MAX_INTERVAL_DAYS = 60;
    private final Clock clock;

    public SpacingPolicy(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.clock = clock;
    }

    public Decision evaluate(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("spacing input is required");
        }
        Instant now = clock.instant();
        if (input.lastReviewedAt().isAfter(now)) {
            throw new IllegalArgumentException("lastReviewedAt must not be in the future");
        }
        int intervalDays = intervalDays(input);
        Instant dueAt = input.lastReviewedAt().plus(Duration.ofDays(intervalDays));
        boolean due = !now.isBefore(dueAt);
        BigDecimal elapsedDays = BigDecimal.valueOf(Math.max(0L,
                Duration.between(input.lastReviewedAt(), now).toHours()))
                .divide(new BigDecimal("24"), 4, RoundingMode.HALF_UP);
        BigDecimal confidencePenalty = new BigDecimal("1.2500")
                .subtract(input.confidence().multiply(new BigDecimal("0.5000")));
        BigDecimal forgettingRisk = ProbabilitySupport.clamp(elapsedDays
                .divide(BigDecimal.valueOf(intervalDays), 6, RoundingMode.HALF_UP)
                .multiply(confidencePenalty));
        PolicyReasonCode reason = input.recallQuality() == RecallQuality.FAILED
                ? PolicyReasonCode.REVIEW_RESET_AFTER_FAILURE
                : due ? PolicyReasonCode.REVIEW_DUE : PolicyReasonCode.REVIEW_NOT_DUE;
        return new Decision(dueAt, forgettingRisk, due, reason, PedagogicalPolicyVersion.V2_P0_1);
    }

    private static int intervalDays(Input input) {
        if (input.recallQuality() == RecallQuality.FAILED) {
            return 1;
        }
        int exponent = Math.min(input.completedReviews(), 5);
        BigDecimal baseDays = BigDecimal.valueOf(1L << exponent);
        BigDecimal qualityMultiplier = switch (input.recallQuality()) {
            case EFFORTFUL -> new BigDecimal("0.7500");
            case SUCCESSFUL -> new BigDecimal("1.2500");
            case EASY -> new BigDecimal("1.7500");
            case FAILED -> BigDecimal.ONE;
        };
        BigDecimal confidenceMultiplier = new BigDecimal("0.7500")
                .add(input.confidence().multiply(new BigDecimal("0.5000")));
        int days = baseDays.multiply(qualityMultiplier).multiply(confidenceMultiplier)
                .setScale(0, RoundingMode.CEILING)
                .intValueExact();
        return Math.max(1, Math.min(MAX_INTERVAL_DAYS, days));
    }

    public enum RecallQuality {
        FAILED,
        EFFORTFUL,
        SUCCESSFUL,
        EASY
    }

    public record Input(
            Instant lastReviewedAt,
            RecallQuality recallQuality,
            BigDecimal confidence,
            int completedReviews
    ) {
        public Input {
            if (lastReviewedAt == null || recallQuality == null) {
                throw new IllegalArgumentException("lastReviewedAt and recallQuality are required");
            }
            confidence = ProbabilitySupport.require(confidence, "confidence");
            if (completedReviews < 0) {
                throw new IllegalArgumentException("completedReviews must not be negative");
            }
        }
    }

    public record Decision(
            Instant dueAt,
            BigDecimal forgettingRisk,
            boolean reviewDue,
            PolicyReasonCode reasonCode,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (dueAt == null || reasonCode == null || policyVersion == null) {
                throw new IllegalArgumentException("spacing decision fields are required");
            }
            forgettingRisk = ProbabilitySupport.require(forgettingRisk, "forgettingRisk");
        }
    }
}
