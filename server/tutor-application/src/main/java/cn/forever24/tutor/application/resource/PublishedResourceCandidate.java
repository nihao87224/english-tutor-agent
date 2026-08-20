package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceType;

import java.util.List;
import java.util.Set;

public record PublishedResourceCandidate(
        String resourceKey,
        String semanticVersion,
        String providerCode,
        String collectionKey,
        ResourceType resourceType,
        String title,
        CefrLevel level,
        String topic,
        String scene,
        String communicationGoal,
        AccessScope accessScope,
        int estimatedMinutes,
        Set<String> skillUnitVariantKeys,
        ResourceAsset taskHero,
        List<ResourceAsset> assets
) {
    public PublishedResourceCandidate {
        skillUnitVariantKeys = Set.copyOf(skillUnitVariantKeys);
        assets = List.copyOf(assets);
    }
}
