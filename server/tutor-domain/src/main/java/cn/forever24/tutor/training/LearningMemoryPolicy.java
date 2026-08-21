package cn.forever24.tutor.training;

import cn.forever24.tutor.planning.policy.SpacingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/** Deterministic transitions for the learner-memory projection of accepted evidence. */
public final class LearningMemoryPolicy {

    private static final BigDecimal INDEPENDENT_THRESHOLD = new BigDecimal("0.8000");
    private static final BigDecimal DAILY_CONFIDENCE_DECAY = new BigDecimal("0.0100");

    private LearningMemoryPolicy() {
    }

    public static String normalizeExpression(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("expression is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normalized.length() > 300) {
            throw new IllegalArgumentException("expression exceeds its limit");
        }
        return normalized;
    }

    public static ExpressionState nextExpressionState(ExpressionState current, BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
        if (current == null) {
            return ExpressionState.PROMPTED;
        }
        if (current == ExpressionState.PROMPTED && score.compareTo(INDEPENDENT_THRESHOLD) >= 0) {
            return ExpressionState.INDEPENDENT;
        }
        return current;
    }

    public static BigDecimal nextConfidence(BigDecimal previous, BigDecimal score) {
        if (previous == null) {
            return score.setScale(5, RoundingMode.HALF_UP);
        }
        return previous.multiply(new BigDecimal("0.70000")).add(score.multiply(new BigDecimal("0.30000")))
                .min(BigDecimal.ONE).setScale(5, RoundingMode.HALF_UP);
    }

    public static BigDecimal decayedConfidence(BigDecimal confidence, Instant lastUsedAt, Instant now) {
        if (confidence == null || lastUsedAt == null || now == null || lastUsedAt.isAfter(now)) {
            throw new IllegalArgumentException("valid confidence and chronological timestamps are required");
        }
        long days = Math.max(0, Duration.between(lastUsedAt, now).toDays());
        return confidence.subtract(DAILY_CONFIDENCE_DECAY.multiply(BigDecimal.valueOf(days)))
                .max(BigDecimal.ZERO).setScale(5, RoundingMode.HALF_UP);
    }

    public static SpacingPolicy.RecallQuality recallQuality(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
        if (score.compareTo(BigDecimal.ZERO) == 0) return SpacingPolicy.RecallQuality.FAILED;
        if (score.compareTo(new BigDecimal("0.6000")) < 0) return SpacingPolicy.RecallQuality.EFFORTFUL;
        if (score.compareTo(new BigDecimal("0.9000")) < 0) return SpacingPolicy.RecallQuality.SUCCESSFUL;
        return SpacingPolicy.RecallQuality.EASY;
    }

    public enum ExpressionState {
        UNDERSTOOD,
        PROMPTED,
        INDEPENDENT,
        TRANSFERRED
    }
}
