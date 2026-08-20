package cn.forever24.tutor.experience;

public record Season(
        String seasonKey,
        String title,
        ExperienceStatus status,
        String metadataJson
) {
    public Season {
        seasonKey = ExperienceValidation.requiredText(seasonKey, "seasonKey", 8);
        if (!seasonKey.matches("^S[0-9]{2}$")) {
            throw new IllegalArgumentException("seasonKey must match S00");
        }
        title = ExperienceValidation.requiredText(title, "season title", 160);
        if (status == null) {
            throw new IllegalArgumentException("season status is required");
        }
        metadataJson = ExperienceValidation.requiredText(metadataJson, "season metadataJson", 10_000);
    }
}
