package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Learner-memory projection intentionally contains keys and states, never raw attempt text. */
public record LearnerMemory(
        List<WeakPoint> weakPoints,
        List<Expression> expressions,
        List<DueReview> dueReviews
) {
    public LearnerMemory {
        weakPoints = List.copyOf(weakPoints == null ? List.of() : weakPoints);
        expressions = List.copyOf(expressions == null ? List.of() : expressions);
        dueReviews = List.copyOf(dueReviews == null ? List.of() : dueReviews);
    }

    public static LearnerMemory empty() {
        return new LearnerMemory(List.of(), List.of(), List.of());
    }

    public record WeakPoint(String errorTag, String skillKey, int frequency, String severity, Instant lastOccurredAt) {
        public WeakPoint {
            if (errorTag == null || errorTag.isBlank() || skillKey == null || skillKey.isBlank()
                    || frequency < 1 || severity == null || severity.isBlank() || lastOccurredAt == null) {
                throw new IllegalArgumentException("valid weak point fields are required");
            }
        }
    }

    public record Expression(String normalizedExpression, ExpressionState state, BigDecimal confidence, Instant lastUsedAt) {
        public Expression {
            if (normalizedExpression == null || normalizedExpression.isBlank() || state == null
                    || confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
                    || confidence.compareTo(BigDecimal.ONE) > 0 || lastUsedAt == null) {
                throw new IllegalArgumentException("valid expression memory fields are required");
            }
        }
    }

    public record DueReview(String targetType, String targetKey, Instant dueAt, BigDecimal forgettingRisk) {
        public DueReview {
            if (targetType == null || targetType.isBlank() || targetKey == null || targetKey.isBlank() || dueAt == null
                    || forgettingRisk == null || forgettingRisk.compareTo(BigDecimal.ZERO) < 0
                    || forgettingRisk.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("valid due review fields are required");
            }
        }
    }
}
