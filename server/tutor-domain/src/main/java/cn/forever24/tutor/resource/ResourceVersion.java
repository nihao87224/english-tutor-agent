package cn.forever24.tutor.resource;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ResourceVersion(
        String resourceKey,
        String semanticVersion,
        String manifestHash,
        String manifestJson,
        String learnerFitJson,
        String generationMetadataJson,
        Set<String> skillUnitVariantKeys,
        List<AssetReference> assetReferences,
        ResourceVersionStatus status,
        Instant createdAt,
        Instant publishedAt
) {
    public ResourceVersion {
        resourceKey = ResourceValidation.required(resourceKey, "resourceKey");
        semanticVersion = ResourceValidation.semanticVersion(semanticVersion);
        manifestHash = ResourceValidation.contentHash(manifestHash);
        manifestJson = ResourceValidation.required(manifestJson, "manifestJson");
        learnerFitJson = ResourceValidation.required(learnerFitJson, "learnerFitJson");
        generationMetadataJson = ResourceValidation.required(generationMetadataJson, "generationMetadataJson");
        if (skillUnitVariantKeys == null || skillUnitVariantKeys.isEmpty()) {
            throw new IllegalArgumentException("resource version requires Skill Unit Variant references");
        }
        skillUnitVariantKeys = Set.copyOf(skillUnitVariantKeys);
        if (assetReferences == null || assetReferences.isEmpty()) {
            throw new IllegalArgumentException("resource version requires asset references");
        }
        assetReferences = List.copyOf(assetReferences);
        Set<String> uniqueAssetKeys = new HashSet<>();
        for (AssetReference reference : assetReferences) {
            if (!uniqueAssetKeys.add(reference.assetKey())) {
                throw new IllegalArgumentException("duplicate asset reference: " + reference.assetKey());
            }
        }
        if (status == null || createdAt == null) {
            throw new IllegalArgumentException("resource version status and createdAt are required");
        }
        if (status == ResourceVersionStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("published resource version requires publishedAt");
        }
    }
}
