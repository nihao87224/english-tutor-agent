package cn.forever24.tutor.experience;

import java.util.Set;

public record ExperienceFitInputs(
        Set<String> goalTags,
        Set<String> topicTags,
        Set<String> interactionTags,
        Set<String> contraindications
) {
    public ExperienceFitInputs {
        goalTags = ExperienceValidation.tags(goalTags, "goalTags", true);
        topicTags = ExperienceValidation.tags(topicTags, "topicTags", true);
        interactionTags = ExperienceValidation.tags(interactionTags, "interactionTags", true);
        contraindications = ExperienceValidation.tags(contraindications, "contraindications", false);
    }
}
