package cn.forever24.tutor.experience;

public record StoryTransition(
        String entryContext,
        String completionBeat,
        boolean continuityRequired
) {
    public StoryTransition {
        entryContext = ExperienceValidation.requiredText(entryContext, "entryContext", 500);
        completionBeat = ExperienceValidation.requiredText(completionBeat, "completionBeat", 500);
    }
}
