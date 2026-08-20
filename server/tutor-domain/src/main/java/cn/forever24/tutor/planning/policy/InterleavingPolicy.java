package cn.forever24.tutor.planning.policy;

import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.ScoredCandidate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InterleavingPolicy {

    public Decision compose(List<ScoredCandidate> rankedCandidates, int timeBudgetMinutes) {
        if (rankedCandidates == null || timeBudgetMinutes < 1 || timeBudgetMinutes > 480) {
            throw new IllegalArgumentException("rankedCandidates and a valid time budget are required");
        }
        List<ScoredCandidate> ranked = new ArrayList<>(rankedCandidates);
        if (ranked.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("rankedCandidates must not contain null");
        }
        if (ranked.stream().map(value -> value.candidate().candidateKey()).distinct().count() != ranked.size()) {
            throw new IllegalArgumentException("ranked candidate keys must be unique");
        }
        ranked.sort(PrescriptionRankingPolicy.scoredComparator());

        ScoredCandidate requiredOutput = ranked.stream()
                .filter(value -> value.candidate().blockType() == BlockType.OUTPUT)
                .filter(value -> value.candidate().estimatedMinutes() <= timeBudgetMinutes)
                .findFirst()
                .orElse(null);
        if (requiredOutput == null) {
            boolean outputExists = ranked.stream()
                    .anyMatch(value -> value.candidate().blockType() == BlockType.OUTPUT);
            return new Decision(
                    List.of(),
                    0,
                    false,
                    List.of(outputExists ? PolicyReasonCode.TIME_BUDGET_EXCEEDED : PolicyReasonCode.OUTPUT_REQUIRED),
                    PedagogicalPolicyVersion.V2_P0_1);
        }

        Set<String> selectedKeys = new LinkedHashSet<>();
        selectedKeys.add(requiredOutput.candidate().candidateKey());
        int usedMinutes = requiredOutput.candidate().estimatedMinutes();
        for (ScoredCandidate candidate : ranked) {
            if (selectedKeys.contains(candidate.candidate().candidateKey())) {
                continue;
            }
            int candidateMinutes = candidate.candidate().estimatedMinutes();
            if (usedMinutes + candidateMinutes <= timeBudgetMinutes) {
                selectedKeys.add(candidate.candidate().candidateKey());
                usedMinutes += candidateMinutes;
            }
        }

        List<ScoredCandidate> selected = ranked.stream()
                .filter(value -> selectedKeys.contains(value.candidate().candidateKey()))
                .toList();
        List<ScoredCandidate> interleaved = interleave(selected);
        return new Decision(
                interleaved,
                usedMinutes,
                true,
                List.of(PolicyReasonCode.INTERLEAVED),
                PedagogicalPolicyVersion.V2_P0_1);
    }

    private static List<ScoredCandidate> interleave(List<ScoredCandidate> selected) {
        List<ScoredCandidate> remaining = new ArrayList<>(selected);
        List<ScoredCandidate> result = new ArrayList<>();
        String previousSkill = null;
        while (!remaining.isEmpty()) {
            ScoredCandidate next = firstWithDifferentSkill(remaining, previousSkill);
            result.add(next);
            previousSkill = next.candidate().skillKey();
            remaining.remove(next);
        }
        return List.copyOf(result);
    }

    private static ScoredCandidate firstWithDifferentSkill(
            List<ScoredCandidate> remaining,
            String previousSkill
    ) {
        if (previousSkill == null) {
            return remaining.getFirst();
        }
        return remaining.stream()
                .filter(value -> !value.candidate().skillKey().equals(previousSkill))
                .findFirst()
                .orElse(remaining.getFirst());
    }

    public record Decision(
            List<ScoredCandidate> blocks,
            int totalMinutes,
            boolean composable,
            List<PolicyReasonCode> reasonCodes,
            PedagogicalPolicyVersion policyVersion
    ) {
        public Decision {
            if (blocks == null || reasonCodes == null || reasonCodes.isEmpty() || policyVersion == null) {
                throw new IllegalArgumentException("interleaving decision fields are required");
            }
            if (totalMinutes < 0) {
                throw new IllegalArgumentException("totalMinutes must not be negative");
            }
            blocks = List.copyOf(blocks);
            reasonCodes = List.copyOf(reasonCodes);
        }
    }
}
