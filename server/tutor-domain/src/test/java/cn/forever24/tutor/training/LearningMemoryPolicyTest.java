package cn.forever24.tutor.training;

import cn.forever24.tutor.planning.policy.SpacingPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningMemoryPolicyTest {

    @Test
    void normalizesExpressionsAndPromotesOnlyAfterStrongSubsequentEvidence() {
        assertEquals("could you help me?", LearningMemoryPolicy.normalizeExpression(" Could   You HELP me? "));
        assertEquals(LearningMemoryPolicy.ExpressionState.PROMPTED,
                LearningMemoryPolicy.nextExpressionState(null, new BigDecimal("1.00000")));
        assertEquals(LearningMemoryPolicy.ExpressionState.PROMPTED,
                LearningMemoryPolicy.nextExpressionState(LearningMemoryPolicy.ExpressionState.PROMPTED, new BigDecimal("0.75000")));
        assertEquals(LearningMemoryPolicy.ExpressionState.INDEPENDENT,
                LearningMemoryPolicy.nextExpressionState(LearningMemoryPolicy.ExpressionState.PROMPTED, new BigDecimal("0.80000")));
    }

    @Test
    void confidenceDecaysDeterministicallyAndScoreMapsToExplicitReviewQuality() {
        assertEquals(new BigDecimal("0.80000"), LearningMemoryPolicy.decayedConfidence(new BigDecimal("0.90000"),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-08-11T00:00:00Z")));
        assertEquals(SpacingPolicy.RecallQuality.FAILED, LearningMemoryPolicy.recallQuality(BigDecimal.ZERO));
        assertEquals(SpacingPolicy.RecallQuality.EFFORTFUL, LearningMemoryPolicy.recallQuality(new BigDecimal("0.40000")));
        assertEquals(SpacingPolicy.RecallQuality.SUCCESSFUL, LearningMemoryPolicy.recallQuality(new BigDecimal("0.80000")));
        assertEquals(SpacingPolicy.RecallQuality.EASY, LearningMemoryPolicy.recallQuality(BigDecimal.ONE));
    }
}
