package cn.forever24.tutor.planning.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PrescriptionRankingPolicy {

    private static final BigDecimal GOAL_MATCH_WEIGHT = new BigDecimal("0.1800");
    private static final BigDecimal SKILL_GAP_WEIGHT = new BigDecimal("0.1800");
    private static final BigDecimal REVIEW_URGENCY_WEIGHT = new BigDecimal("0.1700");
    private static final BigDecimal ERROR_MATCH_WEIGHT = new BigDecimal("0.1000");
    private static final BigDecimal DIFFICULTY_FIT_WEIGHT = new BigDecimal("0.1200");
    private static final BigDecimal TRANSFER_VALUE_WEIGHT = new BigDecimal("0.0800");
    private static final BigDecimal FRESHNESS_WEIGHT = new BigDecimal("0.0600");
    private static final BigDecimal TIME_FIT_WEIGHT = new BigDecimal("0.0600");
    private static final BigDecimal USER_PREFERENCE_WEIGHT = new BigDecimal("0.0500");

    public RankingResult rank(List<Candidate> candidates) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates are required");
        }
        Map<String, PolicyReasonCode> excluded = new LinkedHashMap<>();
        List<ScoredCandidate> scored = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate == null) {
                throw new IllegalArgumentException("candidates must not contain null");
            }
            if (!candidate.factors().accessAllowed()) {
                excluded.put(candidate.candidateKey(), PolicyReasonCode.ACCESS_DENIED);
                continue;
            }
            scored.add(new ScoredCandidate(
                    candidate,
                    score(candidate.factors()),
                    List.of(PolicyReasonCode.RANKED_BY_PEDAGOGICAL_SCORE),
                    PedagogicalPolicyVersion.V2_P0_1));
        }
        if (scored.stream().map(value -> value.candidate().candidateKey()).distinct().count() != scored.size()
                || excluded.size() + scored.size() != candidates.size()) {
            throw new IllegalArgumentException("candidateKey must be unique");
        }
        scored.sort(scoredComparator());
        return new RankingResult(scored, excluded, PedagogicalPolicyVersion.V2_P0_1);
    }

    static Comparator<ScoredCandidate> scoredComparator() {
        return Comparator.comparing(ScoredCandidate::score).reversed()
                .thenComparing(value -> value.candidate().experienceContinuity(), Comparator.reverseOrder())
                .thenComparing(value -> value.candidate().candidateKey());
    }

    private static BigDecimal score(Factors factors) {
        return factors.goalMatch().multiply(GOAL_MATCH_WEIGHT)
                .add(factors.skillGap().multiply(SKILL_GAP_WEIGHT))
                .add(factors.reviewUrgency().multiply(REVIEW_URGENCY_WEIGHT))
                .add(factors.errorMatch().multiply(ERROR_MATCH_WEIGHT))
                .add(factors.difficultyFit().multiply(DIFFICULTY_FIT_WEIGHT))
                .add(factors.transferValue().multiply(TRANSFER_VALUE_WEIGHT))
                .add(factors.freshness().multiply(FRESHNESS_WEIGHT))
                .add(factors.timeFit().multiply(TIME_FIT_WEIGHT))
                .add(factors.userPreference().multiply(USER_PREFERENCE_WEIGHT))
                .setScale(4, RoundingMode.HALF_UP);
    }

    public enum BlockType {
        REVIEW,
        ACQUISITION,
        OUTPUT,
        TRANSFER
    }

    public record Factors(
            BigDecimal goalMatch,
            BigDecimal skillGap,
            BigDecimal reviewUrgency,
            BigDecimal errorMatch,
            BigDecimal difficultyFit,
            BigDecimal transferValue,
            BigDecimal freshness,
            BigDecimal timeFit,
            BigDecimal userPreference,
            boolean accessAllowed
    ) {
        public Factors {
            goalMatch = ProbabilitySupport.require(goalMatch, "goalMatch");
            skillGap = ProbabilitySupport.require(skillGap, "skillGap");
            reviewUrgency = ProbabilitySupport.require(reviewUrgency, "reviewUrgency");
            errorMatch = ProbabilitySupport.require(errorMatch, "errorMatch");
            difficultyFit = ProbabilitySupport.require(difficultyFit, "difficultyFit");
            transferValue = ProbabilitySupport.require(transferValue, "transferValue");
            freshness = ProbabilitySupport.require(freshness, "freshness");
            timeFit = ProbabilitySupport.require(timeFit, "timeFit");
            userPreference = ProbabilitySupport.require(userPreference, "userPreference");
        }
    }

    public record Candidate(
            String candidateKey,
            String skillKey,
            BlockType blockType,
            int estimatedMinutes,
            Factors factors,
            BigDecimal experienceContinuity
    ) {
        public Candidate {
            if (candidateKey == null || candidateKey.isBlank() || skillKey == null || skillKey.isBlank()) {
                throw new IllegalArgumentException("candidateKey and skillKey are required");
            }
            candidateKey = candidateKey.strip();
            skillKey = skillKey.strip();
            if (blockType == null || factors == null) {
                throw new IllegalArgumentException("blockType and factors are required");
            }
            if (estimatedMinutes < 1 || estimatedMinutes > 180) {
                throw new IllegalArgumentException("estimatedMinutes must be between 1 and 180");
            }
            experienceContinuity = ProbabilitySupport.require(experienceContinuity, "experienceContinuity");
        }
    }

    public record ScoredCandidate(
            Candidate candidate,
            BigDecimal score,
            List<PolicyReasonCode> reasonCodes,
            PedagogicalPolicyVersion policyVersion
    ) {
        public ScoredCandidate {
            if (candidate == null || reasonCodes == null || reasonCodes.isEmpty() || policyVersion == null) {
                throw new IllegalArgumentException("scored candidate fields are required");
            }
            score = ProbabilitySupport.require(score, "score");
            reasonCodes = List.copyOf(reasonCodes);
        }
    }

    public record RankingResult(
            List<ScoredCandidate> rankedCandidates,
            Map<String, PolicyReasonCode> excludedCandidates,
            PedagogicalPolicyVersion policyVersion
    ) {
        public RankingResult {
            if (rankedCandidates == null || excludedCandidates == null || policyVersion == null) {
                throw new IllegalArgumentException("ranking result fields are required");
            }
            rankedCandidates = List.copyOf(rankedCandidates);
            excludedCandidates = Map.copyOf(excludedCandidates);
        }
    }
}
