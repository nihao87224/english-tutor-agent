package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.EvidenceSummary;
import cn.forever24.tutor.application.training.LessonEvidenceRepository;
import cn.forever24.tutor.application.planning.LearnerMemory;
import cn.forever24.tutor.planning.policy.SpacingPolicy;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LearningMemoryPolicy;
import cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLessonEvidenceRepository implements LessonEvidenceRepository {
    private final Map<String, EvidenceSummary> values = new ConcurrentHashMap<>();
    private final Map<String, MemoryProjection> memories = new ConcurrentHashMap<>();
    private final SpacingPolicy spacingPolicy;

    public InMemoryLessonEvidenceRepository(Clock clock) {
        this.spacingPolicy = new SpacingPolicy(clock);
    }

    @Override
    public EvidenceSummary record(UserKey userKey, LessonSession session, LessonAttempt attempt) {
        return values.computeIfAbsent(userKey.value() + "|" + attempt.attemptId(), ignored -> {
            var skills = java.util.List.of("speaking");
            long failed = attempt.analysis().criteria().stream().filter(value -> !value.satisfied()).count();
            recordMemory(userKey, attempt, BigDecimal.valueOf(attempt.analysis().criteria().size() - failed)
                    .divide(BigDecimal.valueOf(attempt.analysis().criteria().size()), 5, java.math.RoundingMode.HALF_UP));
            String nextFocus = failed == 0 ? "Use this communication pattern in a new situation."
                    : "Revisit the feedback before trying this communication goal again.";
            return new EvidenceSummary(attempt.attemptId(), 1, skills, nextFocus);
        });
    }

    /** Test/runtime fallback projection with the same replay and retention behavior as the JDBC repository. */
    public LearnerMemory learnerMemory(UserKey userKey, Instant now) {
        MemoryProjection projection = memories.get(userKey.value());
        if (projection == null) return LearnerMemory.empty();
        return projection.snapshot(now);
    }

    private void recordMemory(UserKey userKey, LessonAttempt attempt, BigDecimal score) {
        memories.computeIfAbsent(userKey.value(), ignored -> new MemoryProjection()).record(attempt, score);
    }

    private final class MemoryProjection {
        private final Map<String, Error> errors = new ConcurrentHashMap<>();
        private final Map<String, Expression> expressions = new ConcurrentHashMap<>();
        private final Map<String, Review> reviews = new ConcurrentHashMap<>();

        private synchronized void record(LessonAttempt attempt, BigDecimal score) {
            for (var correction : attempt.analysis().corrections()) {
                String tag = correction.category().strip().toLowerCase(java.util.Locale.ROOT);
                errors.compute(tag, (ignored, current) -> current == null
                        ? new Error(tag, 1, correction.critical() ? "HIGH" : "MEDIUM", attempt.submittedAt())
                        : new Error(tag, current.frequency() + 1,
                        "HIGH".equals(current.severity()) || correction.critical() ? "HIGH" : "MEDIUM", attempt.submittedAt()));
            }
            if (attempt.analysis().corrections().isEmpty() && score.compareTo(new BigDecimal("0.90000")) >= 0) {
                errors.clear();
            }
            updateReview("SKILL", "speaking", new BigDecimal("0.65000"), score, attempt.submittedAt());
            for (String source : attempt.analysis().naturalExpressions().stream().distinct().toList()) {
                String normalized = LearningMemoryPolicy.normalizeExpression(source);
                Expression current = expressions.get(normalized);
                Expression next = new Expression(normalized,
                        LearningMemoryPolicy.nextExpressionState(current == null ? null : current.state(), score),
                        LearningMemoryPolicy.nextConfidence(current == null ? null : current.confidence(), score), attempt.submittedAt());
                expressions.put(normalized, next);
                updateReview("EXPRESSION", normalized, next.confidence(), score, attempt.submittedAt());
            }
        }

        private void updateReview(String type, String target, BigDecimal confidence, BigDecimal score, Instant reviewedAt) {
            String key = type + "|" + target;
            Review current = reviews.get(key);
            int count = current == null ? 0 : current.reviewCount();
            var quality = LearningMemoryPolicy.recallQuality(score);
            var decision = spacingPolicy.evaluate(new SpacingPolicy.Input(reviewedAt, quality, confidence, count));
            reviews.put(key, new Review(type, target, decision.dueAt(), decision.forgettingRisk(), count + 1));
        }

        private LearnerMemory snapshot(Instant now) {
            List<LearnerMemory.WeakPoint> weakPoints = errors.values().stream().map(value ->
                    new LearnerMemory.WeakPoint(value.tag(), "speaking", value.frequency(), value.severity(), value.lastOccurredAt()))
                    .sorted(Comparator.comparing(LearnerMemory.WeakPoint::frequency).reversed()).toList();
            List<LearnerMemory.Expression> expressionList = expressions.values().stream().map(value ->
                    new LearnerMemory.Expression(value.normalized(), value.state(),
                            LearningMemoryPolicy.decayedConfidence(value.confidence(), value.lastUsedAt(), now), value.lastUsedAt()))
                    .sorted(Comparator.comparing(LearnerMemory.Expression::normalizedExpression)).toList();
            List<LearnerMemory.DueReview> due = reviews.values().stream().filter(value -> !value.dueAt().isAfter(now)).map(value ->
                    new LearnerMemory.DueReview(value.type(), value.target(), value.dueAt(), value.forgettingRisk()))
                    .sorted(Comparator.comparing(LearnerMemory.DueReview::dueAt)).toList();
            return new LearnerMemory(weakPoints, expressionList, due);
        }
    }

    private record Error(String tag, int frequency, String severity, Instant lastOccurredAt) { }
    private record Expression(String normalized, ExpressionState state, BigDecimal confidence, Instant lastUsedAt) { }
    private record Review(String type, String target, Instant dueAt, BigDecimal forgettingRisk, int reviewCount) { }
}
