package cn.forever24.tutor.experience;

public record MappingResourceReference(
        String resourceKey,
        String resourceVersion,
        int priority
) {
    public MappingResourceReference {
        resourceKey = ExperienceValidation.externalKey(resourceKey, "resourceKey", 180);
        resourceVersion = ExperienceValidation.semanticVersion(resourceVersion);
        if (priority < 0) {
            throw new IllegalArgumentException("resource priority must not be negative");
        }
    }
}
