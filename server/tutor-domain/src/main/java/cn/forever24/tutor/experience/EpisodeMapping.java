package cn.forever24.tutor.experience;

import cn.forever24.tutor.curriculum.CefrLevel;

import java.util.List;
import java.util.Set;

public record EpisodeMapping(
        String mappingKey,
        String skillUnitVariantKey,
        String seasonKey,
        String episodeKey,
        String sceneKey,
        Set<CefrLevel> eligibleLevels,
        StoryTransition storyTransition,
        ExperienceFitInputs fitInputs,
        String fallbackMappingKey,
        ExperienceStatus status,
        List<MappingResourceReference> resources
) {
    public EpisodeMapping {
        mappingKey = ExperienceValidation.externalKey(mappingKey, "mappingKey", 180);
        skillUnitVariantKey = ExperienceValidation.externalKey(
                skillUnitVariantKey, "skillUnitVariantKey", 192);
        seasonKey = ExperienceValidation.requiredText(seasonKey, "seasonKey", 8);
        episodeKey = ExperienceValidation.requiredText(episodeKey, "episodeKey", 8);
        sceneKey = ExperienceValidation.requiredText(sceneKey, "sceneKey", 32);
        if (eligibleLevels == null || eligibleLevels.isEmpty()
                || eligibleLevels.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("eligibleLevels must not be empty");
        }
        eligibleLevels = Set.copyOf(eligibleLevels);
        if (storyTransition == null || fitInputs == null || status == null) {
            throw new IllegalArgumentException("transition, fit inputs and status are required");
        }
        if (fallbackMappingKey != null) {
            fallbackMappingKey = ExperienceValidation.externalKey(
                    fallbackMappingKey, "fallbackMappingKey", 180);
            if (fallbackMappingKey.equals(mappingKey)) {
                throw new IllegalArgumentException("mapping cannot fall back to itself");
            }
        }
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("mapping requires at least one resource version");
        }
        resources = List.copyOf(resources);
        if (resources.stream().map(MappingResourceReference::resourceKey).distinct().count() != resources.size()) {
            throw new IllegalArgumentException("mapping resource keys must be unique");
        }
        if (resources.stream().map(MappingResourceReference::priority).distinct().count() != resources.size()) {
            throw new IllegalArgumentException("mapping resource priorities must be unique");
        }
    }
}
