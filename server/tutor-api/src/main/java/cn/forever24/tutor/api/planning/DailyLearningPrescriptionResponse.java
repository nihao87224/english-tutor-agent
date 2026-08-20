package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.PrescriptionBlock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailyLearningPrescriptionResponse(
        String prescriptionId,
        long version,
        LocalDate learningDate,
        String timezone,
        String status,
        GoalResponse priorityGoal,
        String rationale,
        List<String> reasonCodes,
        int estimatedMinutes,
        ExperienceResponse experience,
        List<BlockResponse> blocks,
        Instant generatedAt,
        Instant expiresAt
) {

    public static DailyLearningPrescriptionResponse from(DailyLearningPrescription prescription) {
        PrescriptionBlock first = prescription.blocks().getFirst();
        return new DailyLearningPrescriptionResponse(
                prescription.prescriptionId(),
                prescription.version(),
                prescription.learningDate(),
                prescription.learnerZone().getId(),
                prescription.status().name(),
                new GoalResponse(prescription.priorityGoal().code(), prescription.priorityGoal().label()),
                prescription.rationale(),
                prescription.reasonCodes(),
                prescription.estimatedMinutes(),
                new ExperienceResponse(first.seasonKey(), first.episodeKey(), first.sceneKey(), first.title()),
                prescription.blocks().stream().map(BlockResponse::from).toList(),
                prescription.generatedAt(),
                prescription.expiresAt());
    }

    public record GoalResponse(String code, String label) {
    }

    public record ExperienceResponse(String seasonId, String episodeId, String sceneId, String title) {
    }

    public record ResourceResponse(String resourceId, String resourceVersion) {
    }

    public record FocalPointResponse(BigDecimal x, BigDecimal y) {
    }

    public record TaskHeroResponse(
            String assetId,
            String url,
            String aspectRatio,
            FocalPointResponse focalPoint,
            String altText
    ) {
    }

    public record BlockResponse(
            String blockId,
            int sequence,
            String type,
            String title,
            String skillUnitVariantId,
            ResourceResponse resource,
            String episodeMappingId,
            String difficulty,
            String scaffolding,
            String trainingType,
            int estimatedMinutes,
            List<String> expectedEvidence,
            ResourceResponse fallbackResource,
            Map<String, BigDecimal> recommendationFactors,
            TaskHeroResponse taskHero,
            String status
    ) {

        private static BlockResponse from(PrescriptionBlock block) {
            ResourceResponse fallback = block.fallback() == null ? null : new ResourceResponse(
                    block.fallback().resourceKey(), block.fallback().resourceVersion());
            return new BlockResponse(
                    block.blockId(),
                    block.sequence(),
                    block.type().name(),
                    block.title(),
                    block.skillUnitVariantKey(),
                    new ResourceResponse(block.resource().resourceKey(), block.resource().resourceVersion()),
                    block.episodeMappingKey(),
                    block.difficulty().name(),
                    block.scaffolding().name(),
                    block.trainingType().name(),
                    block.estimatedMinutes(),
                    block.expectedEvidence(),
                    fallback,
                    block.recommendationFactors(),
                    new TaskHeroResponse(
                            block.taskHero().assetKey(),
                            block.taskHero().publicUrl(),
                            block.taskHero().aspectRatio(),
                            new FocalPointResponse(
                                    block.taskHero().focalPointX(), block.taskHero().focalPointY()),
                            block.taskHero().altText()),
                    block.status().name());
        }
    }
}
