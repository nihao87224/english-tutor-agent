package cn.forever24.tutor.resource;

import cn.forever24.tutor.curriculum.CefrLevel;

public record LearningResource(
        String resourceKey,
        String providerCode,
        String collectionKey,
        ResourceType type,
        String title,
        String description,
        String language,
        CefrLevel level,
        String topic,
        String scene,
        String communicationGoal,
        AccessScope accessScope,
        PublishStatus publishStatus,
        String activeVersion,
        int estimatedMinutes
) {
    public LearningResource {
        resourceKey = ResourceValidation.required(resourceKey, "resourceKey");
        providerCode = ResourceValidation.required(providerCode, "providerCode");
        collectionKey = ResourceValidation.required(collectionKey, "collectionKey");
        if (type == null) {
            throw new IllegalArgumentException("resource type is required");
        }
        title = ResourceValidation.required(title, "resource title");
        language = ResourceValidation.required(language, "language");
        if (level == null) {
            throw new IllegalArgumentException("resource level is required");
        }
        topic = ResourceValidation.required(topic, "topic");
        scene = ResourceValidation.required(scene, "scene");
        communicationGoal = ResourceValidation.required(communicationGoal, "communicationGoal");
        if (accessScope == null || publishStatus == null) {
            throw new IllegalArgumentException("resource access scope and publish status are required");
        }
        if (activeVersion != null) {
            activeVersion = ResourceValidation.semanticVersion(activeVersion);
        }
        if (publishStatus == PublishStatus.PUBLISHED && activeVersion == null) {
            throw new IllegalArgumentException("published resource requires an active version");
        }
        if (estimatedMinutes <= 0) {
            throw new IllegalArgumentException("estimatedMinutes must be positive");
        }
    }
}
