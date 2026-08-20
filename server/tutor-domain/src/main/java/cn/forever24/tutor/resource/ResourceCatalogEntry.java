package cn.forever24.tutor.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ResourceCatalogEntry(
        ContentProvider provider,
        ResourceCollection collection,
        LearningResource resource,
        ResourceVersion resourceVersion,
        List<ResourceAsset> assets
) {
    public ResourceCatalogEntry {
        if (provider == null || collection == null || resource == null || resourceVersion == null) {
            throw new IllegalArgumentException("provider, collection, resource and version are required");
        }
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("catalog entry requires assets");
        }
        assets = List.copyOf(assets);
        validate(provider, collection, resource, resourceVersion, assets);
    }

    private static void validate(
            ContentProvider provider,
            ResourceCollection collection,
            LearningResource resource,
            ResourceVersion version,
            List<ResourceAsset> assets
    ) {
        if (!provider.providerCode().equals(collection.providerCode())
                || !provider.providerCode().equals(resource.providerCode())) {
            throw new IllegalArgumentException("provider references are inconsistent");
        }
        if (!collection.collectionKey().equals(resource.collectionKey())) {
            throw new IllegalArgumentException("resource collection reference is inconsistent");
        }
        if (provider.type() == ContentProviderType.THIRD_PARTY
                && (collection.licenseNote() == null || collection.licenseNote().isBlank())) {
            throw new IllegalArgumentException("third-party collection requires license metadata");
        }
        if (!resource.resourceKey().equals(version.resourceKey())) {
            throw new IllegalArgumentException("resource version key is inconsistent");
        }
        if (version.semanticVersion().equals(resource.activeVersion())
                && version.status() != ResourceVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("activeVersion must reference a published resource version");
        }

        Map<String, ResourceAsset> assetsByKey = new HashMap<>();
        for (ResourceAsset asset : assets) {
            if (assetsByKey.putIfAbsent(asset.assetKey(), asset) != null) {
                throw new IllegalArgumentException("duplicate asset key: " + asset.assetKey());
            }
        }
        List<ResourceAsset> referencedAssets = version.assetReferences().stream()
                .map(reference -> {
                    ResourceAsset asset = assetsByKey.get(reference.assetKey());
                    if (asset == null) {
                        throw new IllegalArgumentException("unknown asset reference: " + reference.assetKey());
                    }
                    return asset;
                })
                .toList();

        if (resource.type() == ResourceType.SCENARIO_LESSON
                && version.status() == ResourceVersionStatus.PUBLISHED) {
            List<ResourceAsset> taskHeroes = referencedAssets.stream()
                    .filter(asset -> asset.purpose() == AssetPurpose.TASK_HERO)
                    .toList();
            if (taskHeroes.size() != 1) {
                throw new IllegalArgumentException("published Scenario Lesson requires exactly one TASK_HERO");
            }
            ResourceAsset taskHero = taskHeroes.getFirst();
            if (!(taskHero.metadata() instanceof ImageAssetMetadata imageMetadata)
                    || !imageMetadata.supportsTaskHeroPresentation()) {
                throw new IllegalArgumentException(
                        "TASK_HERO must be environmental and cover SCENARIO_INTRO and SCENARIO_TRAINING");
            }
        }
    }
}
