package cn.forever24.tutor.curriculum;

import java.util.List;

public record SkillUnit(
        String skillUnitKey,
        String communicationGoal,
        ReviewTemplate reviewTemplate,
        String semanticVersion,
        CurriculumStatus status,
        List<SkillUnitVariant> variants
) {

    public SkillUnit {
        skillUnitKey = required(skillUnitKey, "skillUnitKey", 160);
        communicationGoal = required(communicationGoal, "communicationGoal", 500);
        semanticVersion = required(semanticVersion, "semanticVersion", 32);
        if (reviewTemplate == null || status == null) {
            throw new IllegalArgumentException("reviewTemplate and status are required");
        }
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("a Skill Unit must contain at least one variant");
        }
        variants = List.copyOf(variants);
        if (variants.stream().map(SkillUnitVariant::variantKey).distinct().count() != variants.size()) {
            throw new IllegalArgumentException("duplicate variant key in Skill Unit");
        }
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        }
        return normalized;
    }
}
