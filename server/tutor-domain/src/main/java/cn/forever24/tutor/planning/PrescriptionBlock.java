package cn.forever24.tutor.planning;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PrescriptionBlock(
        String blockId,
        int sequence,
        BlockType type,
        String title,
        String skillUnitVariantKey,
        PrescriptionResourceRef resource,
        String episodeMappingKey,
        String seasonKey,
        String episodeKey,
        String sceneKey,
        CefrLevel difficulty,
        ScaffoldingLevel scaffolding,
        TrainingType trainingType,
        int estimatedMinutes,
        List<String> expectedEvidence,
        CompletionPolicy completionPolicy,
        PrescriptionResourceRef fallback,
        Map<String, BigDecimal> recommendationFactors,
        PrescriptionTaskHero taskHero,
        PrescriptionBlockStatus status
) {

    public PrescriptionBlock {
        blockId = required(blockId, "blockId", 64);
        if (sequence < 1 || type == null) {
            throw new IllegalArgumentException("valid sequence and block type are required");
        }
        title = required(title, "title", 300);
        skillUnitVariantKey = required(skillUnitVariantKey, "skillUnitVariantKey", 192);
        episodeMappingKey = required(episodeMappingKey, "episodeMappingKey", 180);
        seasonKey = required(seasonKey, "seasonKey", 16);
        episodeKey = required(episodeKey, "episodeKey", 16);
        sceneKey = required(sceneKey, "sceneKey", 64);
        if (resource == null || difficulty == null || scaffolding == null || trainingType == null
                || completionPolicy == null || taskHero == null || status == null) {
            throw new IllegalArgumentException("prescription block metadata is required");
        }
        if (estimatedMinutes < 1 || estimatedMinutes > 180) {
            throw new IllegalArgumentException("estimatedMinutes must be between 1 and 180");
        }
        if (expectedEvidence == null || expectedEvidence.isEmpty()
                || expectedEvidence.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("expectedEvidence must not be empty");
        }
        expectedEvidence = expectedEvidence.stream().map(String::strip).distinct().toList();
        if (recommendationFactors == null || recommendationFactors.isEmpty()) {
            throw new IllegalArgumentException("recommendationFactors must not be empty");
        }
        if (recommendationFactors.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null
                        || entry.getValue().compareTo(BigDecimal.ZERO) < 0
                        || entry.getValue().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("recommendationFactors must contain named values between 0 and 1");
        }
        recommendationFactors = Map.copyOf(recommendationFactors);
    }

    public PrescriptionBlock skipped() {
        return new PrescriptionBlock(
                blockId, sequence, type, title, skillUnitVariantKey, resource, episodeMappingKey,
                seasonKey, episodeKey, sceneKey, difficulty, scaffolding, trainingType,
                estimatedMinutes, expectedEvidence, completionPolicy, fallback,
                recommendationFactors, taskHero, PrescriptionBlockStatus.SKIPPED);
    }

    private static String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.strip().length() > maximumLength) {
            throw new IllegalArgumentException("valid " + field + " is required");
        }
        return value.strip();
    }
}
