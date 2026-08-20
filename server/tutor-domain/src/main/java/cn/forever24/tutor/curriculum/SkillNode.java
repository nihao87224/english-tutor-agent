package cn.forever24.tutor.curriculum;

public record SkillNode(
        String skillKey,
        String name,
        String category,
        String parentSkillKey,
        CefrRange cefrRange,
        int importance,
        CurriculumStatus status
) {

    public SkillNode {
        skillKey = required(skillKey, "skillKey", 128);
        name = required(name, "name", 160);
        category = required(category, "category", 64);
        if (parentSkillKey != null) {
            parentSkillKey = required(parentSkillKey, "parentSkillKey", 128);
        }
        if (cefrRange == null) {
            throw new IllegalArgumentException("cefrRange is required");
        }
        if (importance < 0 || importance > 100) {
            throw new IllegalArgumentException("importance must be between 0 and 100");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (skillKey.equals(parentSkillKey)) {
            throw new IllegalArgumentException("a skill cannot be its own parent");
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
