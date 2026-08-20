package cn.forever24.tutor.experience;

public record Episode(
        String episodeKey,
        String seasonKey,
        String title,
        String storyAnchor,
        boolean storyOrderRequired,
        ExperienceStatus status,
        String metadataJson,
        int sequenceNumber
) {
    public Episode {
        episodeKey = ExperienceValidation.requiredText(episodeKey, "episodeKey", 8);
        if (!episodeKey.matches("^EP[0-9]{3}$")) {
            throw new IllegalArgumentException("episodeKey must match EP000");
        }
        seasonKey = ExperienceValidation.requiredText(seasonKey, "seasonKey", 8);
        title = ExperienceValidation.requiredText(title, "episode title", 160);
        storyAnchor = ExperienceValidation.requiredText(storyAnchor, "storyAnchor", 1000);
        if (storyOrderRequired) {
            throw new IllegalArgumentException("P0 requires storyOrderRequired=false");
        }
        if (status == null) {
            throw new IllegalArgumentException("episode status is required");
        }
        metadataJson = ExperienceValidation.requiredText(metadataJson, "episode metadataJson", 10_000);
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("sequenceNumber must not be negative");
        }
    }
}
