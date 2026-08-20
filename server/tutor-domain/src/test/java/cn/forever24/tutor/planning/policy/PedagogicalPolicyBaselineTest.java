package cn.forever24.tutor.planning.policy;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.Prerequisite;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.Candidate;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.Factors;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.RankingResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedagogicalPolicyBaselineTest {

    private static final PedagogicalPolicyVersion VERSION = PedagogicalPolicyVersion.V2_P0_1;

    @Test
    void blocksVariantWhenPrerequisiteMasteryOrConfidenceIsMissing() {
        PrerequisitePolicy policy = new PrerequisitePolicy();
        List<Prerequisite> prerequisites = List.of(
                new Prerequisite("skill-a", decimal("0.6000"), decimal("0.5000")),
                new Prerequisite("skill-b", decimal("0.7000"), decimal("0.6000")));

        PrerequisitePolicy.Decision decision = policy.evaluate(prerequisites, Map.of(
                "skill-a", new PrerequisitePolicy.SkillState(decimal("0.8000"), decimal("0.7000")),
                "skill-b", new PrerequisitePolicy.SkillState(decimal("0.6500"), decimal("0.9000"))));

        assertFalse(decision.eligible());
        assertEquals(PolicyReasonCode.PREREQUISITE_NOT_MET, decision.reasonCode());
        assertEquals(List.of("skill-b"), decision.unmetSkillKeys());
        assertEquals(VERSION, decision.policyVersion());
    }

    @Test
    void masteredSkillIsEligibleOnlyForReviewUpgradeOrTransfer() {
        MasteryEligibilityPolicy policy = new MasteryEligibilityPolicy();

        assertEquals(MasteryEligibilityPolicy.LearningRoute.NONE,
                policy.evaluate(decimal("0.8000"), false, false, false).route());
        assertEquals(MasteryEligibilityPolicy.LearningRoute.REVIEW,
                policy.evaluate(decimal("0.8500"), true, false, false).route());
        assertEquals(MasteryEligibilityPolicy.LearningRoute.UPGRADE,
                policy.evaluate(decimal("0.8500"), false, true, false).route());
        assertEquals(MasteryEligibilityPolicy.LearningRoute.TRANSFER,
                policy.evaluate(decimal("0.8500"), false, false, true).route());
        assertEquals(MasteryEligibilityPolicy.LearningRoute.ACQUISITION,
                policy.evaluate(decimal("0.7999"), false, false, false).route());
    }

    @Test
    void spacingUsesPerformanceConfidenceHistoryAndExplicitClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
        SpacingPolicy policy = new SpacingPolicy(clock);
        SpacingPolicy.Input input = new SpacingPolicy.Input(
                Instant.parse("2026-08-01T00:00:00Z"),
                SpacingPolicy.RecallQuality.SUCCESSFUL,
                decimal("0.8000"),
                2);

        SpacingPolicy.Decision first = policy.evaluate(input);
        SpacingPolicy.Decision replay = policy.evaluate(input);

        assertEquals(Instant.parse("2026-08-07T00:00:00Z"), first.dueAt());
        assertTrue(first.reviewDue());
        assertEquals(PolicyReasonCode.REVIEW_DUE, first.reasonCode());
        assertEquals(first, replay);
        assertThrows(IllegalArgumentException.class, () -> policy.evaluate(new SpacingPolicy.Input(
                Instant.parse("2026-08-11T00:00:00Z"),
                SpacingPolicy.RecallQuality.EASY,
                decimal("0.8000"),
                1)));
    }

    @Test
    void continuousFailureAddsScaffoldingAndContinuousEaseRaisesDifficulty() {
        DifficultyPolicy policy = new DifficultyPolicy();

        DifficultyPolicy.Decision failed = policy.evaluate(new DifficultyPolicy.Input(CefrLevel.B1, 2, 0));
        DifficultyPolicy.Decision easy = policy.evaluate(new DifficultyPolicy.Input(CefrLevel.B1, 0, 3));

        assertEquals(CefrLevel.B1, failed.targetLevel());
        assertEquals(2, failed.maximumCommunicationComplexity());
        assertEquals(ScaffoldingLevel.HIGH, failed.scaffolding());
        assertEquals(PolicyReasonCode.DIFFICULTY_REDUCED_AFTER_FAILURES, failed.reasonCode());
        assertEquals(CefrLevel.B2, easy.targetLevel());
        assertEquals(4, easy.maximumCommunicationComplexity());
        assertEquals(ScaffoldingLevel.LOW, easy.scaffolding());
    }

    @Test
    void transferRequiresMasteryAndANewContext() {
        TransferPolicy policy = new TransferPolicy();

        assertTrue(policy.evaluate(true, Set.of("airport"), "hotel").transferCandidate());
        assertEquals(PolicyReasonCode.TRANSFER_CONTEXT_ALREADY_USED,
                policy.evaluate(true, Set.of("airport"), "airport").reasonCode());
        assertEquals(PolicyReasonCode.TRANSFER_NOT_READY,
                policy.evaluate(false, Set.of("airport"), "hotel").reasonCode());
    }

    @Test
    void retryAndCompletionKeepCriticalRetrySeparateFromMastery() {
        AttemptRetryPolicy retryPolicy = new AttemptRetryPolicy();

        assertEquals(AttemptRetryPolicy.Action.RETRY,
                retryPolicy.evaluate(new AttemptRetryPolicy.Input(true, 1, 1, 3)).action());
        assertEquals(AttemptRetryPolicy.Action.FINAL_FAILURE,
                retryPolicy.evaluate(new AttemptRetryPolicy.Input(true, 1, 3, 3)).action());
        assertEquals(AttemptRetryPolicy.Action.ACCEPT,
                retryPolicy.evaluate(new AttemptRetryPolicy.Input(false, 1, 1, 3)).action());

        TaskCompletionPolicy completionPolicy = new TaskCompletionPolicy();
        CompletionPolicy curriculumPolicy = new CompletionPolicy(1, Set.of("meaning"), true);
        TaskCompletionPolicy.Decision incomplete = completionPolicy.evaluate(curriculumPolicy,
                new TaskCompletionPolicy.Progress(true, true, 0, Set.of("meaning")));
        TaskCompletionPolicy.Decision completed = completionPolicy.evaluate(curriculumPolicy,
                new TaskCompletionPolicy.Progress(true, true, 1, Set.of("meaning")));

        assertEquals(PolicyReasonCode.OUTPUT_REQUIRED, incomplete.reasonCode());
        assertTrue(completed.completed());
        assertFalse(completed.masteryChanged());
        assertEquals(PolicyReasonCode.COMPLETED_WITHOUT_MASTERY, completed.reasonCode());
    }

    @Test
    void dueReviewOutranksModerateNewContentAndAccessIsFilteredFirst() {
        PrescriptionRankingPolicy policy = new PrescriptionRankingPolicy();
        Candidate dueReview = candidate("review", "skill-review", BlockType.REVIEW, 5,
                factors("0.6000", "0.7000", "1.0000", "0.6000", "0.9000",
                        "0.5000", "0.6000", "1.0000", "0.5000", true),
                "0.0000");
        Candidate newContent = candidate("new", "skill-new", BlockType.ACQUISITION, 10,
                factors("1.0000", "1.0000", "0.0000", "0.0000", "0.8000",
                        "0.0000", "1.0000", "1.0000", "1.0000", true),
                "1.0000");
        Candidate denied = candidate("private", "skill-private", BlockType.OUTPUT, 5,
                allFactors("1.0000", false), "1.0000");

        RankingResult result = policy.rank(List.of(newContent, denied, dueReview));

        assertEquals(List.of("review", "new"), result.rankedCandidates().stream()
                .map(value -> value.candidate().candidateKey()).toList());
        assertEquals(new BigDecimal("0.7330"), result.rankedCandidates().getFirst().score());
        assertEquals(PolicyReasonCode.ACCESS_DENIED, result.excludedCandidates().get("private"));
    }

    @Test
    void episodeContinuityCannotOverridePedagogicalScoreAndOnlyBreaksExactTies() {
        PrescriptionRankingPolicy policy = new PrescriptionRankingPolicy();
        Candidate stronger = candidate("stronger", "skill-a", BlockType.OUTPUT, 5,
                allFactors("0.8000", true), "0.0000");
        Candidate storyNext = candidate("story-next", "skill-b", BlockType.OUTPUT, 5,
                allFactors("0.7000", true), "1.0000");
        Candidate tiedStory = candidate("tied-story", "skill-c", BlockType.OUTPUT, 5,
                allFactors("0.8000", true), "1.0000");

        RankingResult result = policy.rank(List.of(storyNext, stronger, tiedStory));

        assertEquals(List.of("tied-story", "stronger", "story-next"), result.rankedCandidates().stream()
                .map(value -> value.candidate().candidateKey()).toList());
        assertEquals(new BigDecimal("0.8000"), result.rankedCandidates().getFirst().score());
        assertEquals(new BigDecimal("0.8000"), result.rankedCandidates().get(1).score());
    }

    @Test
    void factorNormalizationAndDeterministicReplayAreEnforced() {
        PrescriptionRankingPolicy policy = new PrescriptionRankingPolicy();
        Candidate candidate = candidate("stable", "skill", BlockType.OUTPUT, 5,
                allFactors("0.5000", true), "0.5000");

        RankingResult first = policy.rank(List.of(candidate));
        RankingResult replay = policy.rank(List.of(candidate));

        assertEquals(new BigDecimal("0.5000"), first.rankedCandidates().getFirst().score());
        assertEquals(first, replay);
        assertThrows(IllegalArgumentException.class, () -> allFactors("1.0001", true));
    }

    @Test
    void interleavingHonorsTimeBudgetAndRequiresAnOutputBlock() {
        PrescriptionRankingPolicy rankingPolicy = new PrescriptionRankingPolicy();
        List<Candidate> candidates = List.of(
                candidate("review", "skill-a", BlockType.REVIEW, 5,
                        allFactors("0.9000", true), "0.0000"),
                candidate("acquire-same", "skill-a", BlockType.ACQUISITION, 5,
                        allFactors("0.8000", true), "0.0000"),
                candidate("output", "skill-b", BlockType.OUTPUT, 6,
                        allFactors("0.7000", true), "0.0000"),
                candidate("too-long", "skill-c", BlockType.TRANSFER, 20,
                        allFactors("1.0000", true), "0.0000"));
        RankingResult ranked = rankingPolicy.rank(candidates);

        InterleavingPolicy.Decision decision = new InterleavingPolicy().compose(ranked.rankedCandidates(), 16);

        assertTrue(decision.composable());
        assertEquals(16, decision.totalMinutes());
        assertTrue(decision.blocks().stream().anyMatch(value -> value.candidate().blockType() == BlockType.OUTPUT));
        assertEquals(List.of("review", "output", "acquire-same"), decision.blocks().stream()
                .map(value -> value.candidate().candidateKey()).toList());

        RankingResult noOutput = rankingPolicy.rank(List.of(candidates.getFirst()));
        InterleavingPolicy.Decision rejected = new InterleavingPolicy().compose(noOutput.rankedCandidates(), 10);
        assertFalse(rejected.composable());
        assertEquals(List.of(PolicyReasonCode.OUTPUT_REQUIRED), rejected.reasonCodes());
    }

    private static Candidate candidate(
            String key,
            String skill,
            BlockType type,
            int minutes,
            Factors factors,
            String continuity
    ) {
        return new Candidate(key, skill, type, minutes, factors, decimal(continuity));
    }

    private static Factors allFactors(String value, boolean accessAllowed) {
        return factors(value, value, value, value, value, value, value, value, value, accessAllowed);
    }

    private static Factors factors(
            String goalMatch,
            String skillGap,
            String reviewUrgency,
            String errorMatch,
            String difficultyFit,
            String transferValue,
            String freshness,
            String timeFit,
            String userPreference,
            boolean accessAllowed
    ) {
        return new Factors(
                decimal(goalMatch),
                decimal(skillGap),
                decimal(reviewUrgency),
                decimal(errorMatch),
                decimal(difficultyFit),
                decimal(transferValue),
                decimal(freshness),
                decimal(timeFit),
                decimal(userPreference),
                accessAllowed);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
