package cn.forever24.tutor.experience;

public record Scene(
        String sceneKey,
        String episodeKey,
        String title,
        String location,
        String storyContext,
        String characterStateJson,
        ExperienceStatus status
) {
    public Scene {
        sceneKey = ExperienceValidation.requiredText(sceneKey, "sceneKey", 32);
        if (!sceneKey.matches("^[A-Z0-9][A-Z0-9_-]{2,31}$")) {
            throw new IllegalArgumentException("sceneKey has an invalid format");
        }
        episodeKey = ExperienceValidation.requiredText(episodeKey, "episodeKey", 8);
        title = ExperienceValidation.requiredText(title, "scene title", 160);
        location = ExperienceValidation.requiredText(location, "scene location", 240);
        storyContext = ExperienceValidation.requiredText(storyContext, "storyContext", 1000);
        characterStateJson = ExperienceValidation.requiredText(
                characterStateJson, "characterStateJson", 10_000);
        if (status == null) {
            throw new IllegalArgumentException("scene status is required");
        }
    }
}
