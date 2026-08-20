package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.ResourceType;

public record ResourceCandidateQuery(
        ResourceType resourceType,
        String collectionKey,
        CefrLevel level,
        String skillUnitVariantKey,
        String topic,
        String scene,
        AccessScope accessScope
) {
    public ResourceCandidateQuery(
            CefrLevel level,
            String skillUnitVariantKey,
            String topic,
            String scene,
            AccessScope accessScope
    ) {
        this(null, null, level, skillUnitVariantKey, topic, scene, accessScope);
    }

    public static ResourceCandidateQuery allPublished() {
        return new ResourceCandidateQuery(null, null, null, null, null, null, null);
    }
}
