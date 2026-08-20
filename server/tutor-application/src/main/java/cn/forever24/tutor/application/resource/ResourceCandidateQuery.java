package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AccessScope;

public record ResourceCandidateQuery(
        CefrLevel level,
        String skillUnitVariantKey,
        String topic,
        String scene,
        AccessScope accessScope
) {
    public static ResourceCandidateQuery allPublished() {
        return new ResourceCandidateQuery(null, null, null, null, null);
    }
}
