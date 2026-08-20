package cn.forever24.tutor.experience;

import cn.forever24.tutor.curriculum.CefrLevel;

import java.util.Set;

public record ExperienceResolutionRequest(
        String skillUnitVariantKey,
        CefrLevel learnerLevel,
        Set<String> goalTags,
        Set<String> topicTags,
        Set<String> interactionTags,
        Set<String> contraindications,
        String continuityEpisodeKey,
        String preferredMappingKey
) {
    public ExperienceResolutionRequest {
        skillUnitVariantKey = ExperienceValidation.externalKey(
                skillUnitVariantKey, "skillUnitVariantKey", 192);
        if (learnerLevel == null) {
            throw new IllegalArgumentException("learnerLevel is required");
        }
        goalTags = ExperienceValidation.tags(goalTags, "goalTags", false);
        topicTags = ExperienceValidation.tags(topicTags, "topicTags", false);
        interactionTags = ExperienceValidation.tags(interactionTags, "interactionTags", false);
        contraindications = ExperienceValidation.tags(contraindications, "contraindications", false);
        if (continuityEpisodeKey != null) {
            continuityEpisodeKey = ExperienceValidation.requiredText(
                    continuityEpisodeKey, "continuityEpisodeKey", 8);
        }
        if (preferredMappingKey != null) {
            preferredMappingKey = ExperienceValidation.externalKey(
                    preferredMappingKey, "preferredMappingKey", 180);
        }
    }
}
