package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogWriteOutcome;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.application.resource.ResourceCatalogConflictException;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.application.resource.ResourceVersionSaveResult;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetPurpose;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.ContentProvider;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceVersionStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryResourceCatalogRepository implements ResourceCatalogRepository {

    private final Map<String, ContentProvider> providers = new LinkedHashMap<>();
    private final Map<String, ResourceCollection> collections = new LinkedHashMap<>();
    private final Map<String, LearningResource> resources = new LinkedHashMap<>();
    private final Map<String, ResourceAsset> assets = new LinkedHashMap<>();
    private final Map<VersionKey, ResourceCatalogEntry> versions = new LinkedHashMap<>();

    @Override
    public synchronized ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry) {
        VersionKey key = new VersionKey(entry.resource().resourceKey(), entry.resourceVersion().semanticVersion());
        ResourceCatalogEntry existing = versions.get(key);
        if (existing != null) {
            if (existing.resourceVersion().manifestHash().equals(entry.resourceVersion().manifestHash())) {
                return result(existing, CatalogWriteOutcome.ALREADY_EXISTS);
            }
            throw new ResourceCatalogConflictException(
                    "resource version already exists with a different manifest hash: " + key);
        }

        for (ResourceAsset asset : entry.assets()) {
            ResourceAsset existingAsset = assets.get(asset.assetKey());
            if (existingAsset != null && !sameImmutableAsset(existingAsset, asset)) {
                throw new ResourceCatalogConflictException(
                        "asset key already exists with different immutable metadata: " + asset.assetKey());
            }
        }

        providers.put(entry.provider().providerCode(), entry.provider());
        collections.put(entry.collection().collectionKey(), entry.collection());
        resources.put(entry.resource().resourceKey(), entry.resource());
        entry.assets().forEach(asset -> assets.putIfAbsent(asset.assetKey(), asset));
        versions.put(key, entry);
        return result(entry, CatalogWriteOutcome.CREATED);
    }

    @Override
    public synchronized Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion) {
        ResourceCatalogEntry stored = versions.get(new VersionKey(resourceKey, semanticVersion));
        if (stored == null) {
            return Optional.empty();
        }
        LearningResource currentResource = resources.getOrDefault(resourceKey, stored.resource());
        ResourceCollection currentCollection = collections.getOrDefault(
                currentResource.collectionKey(), stored.collection());
        ContentProvider currentProvider = providers.getOrDefault(
                currentResource.providerCode(), stored.provider());
        List<ResourceAsset> currentAssets = stored.resourceVersion().assetReferences().stream()
                .map(reference -> assets.get(reference.assetKey()))
                .toList();
        return Optional.of(new ResourceCatalogEntry(
                currentProvider,
                currentCollection,
                currentResource,
                stored.resourceVersion(),
                currentAssets));
    }

    @Override
    public synchronized List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query) {
        List<PublishedResourceCandidate> candidates = new ArrayList<>();
        for (Map.Entry<VersionKey, ResourceCatalogEntry> item : versions.entrySet()) {
            ResourceCatalogEntry stored = item.getValue();
            LearningResource resource = resources.get(stored.resource().resourceKey());
            ResourceCollection collection = collections.get(resource.collectionKey());
            if (!isPublishedCandidate(resource, collection, stored, query)) {
                continue;
            }
            List<ResourceAsset> referencedAssets = stored.resourceVersion().assetReferences().stream()
                    .map(reference -> assets.get(reference.assetKey()))
                    .toList();
            if (referencedAssets.stream().anyMatch(asset -> asset == null
                    || asset.status() != AssetStatus.ACTIVE
                    || asset.accessScope() == AccessScope.DISABLED)) {
                continue;
            }
            ResourceAsset taskHero = referencedAssets.stream()
                    .filter(asset -> asset.purpose() == AssetPurpose.TASK_HERO)
                    .findFirst()
                    .orElseThrow();
            candidates.add(toCandidate(resource, stored, taskHero, referencedAssets));
        }
        return candidates.stream()
                .sorted(Comparator.comparing(PublishedResourceCandidate::resourceKey))
                .toList();
    }

    @Override
    public synchronized List<ResourceCatalogEntry> findAllResourceVersions() {
        return versions.keySet().stream()
                .map(key -> findExactVersion(key.resourceKey(), key.semanticVersion()).orElseThrow())
                .toList();
    }

    @Override
    public synchronized List<ResourceCatalogEntry> findResourceVersions(String resourceKey) {
        return versions.keySet().stream()
                .filter(key -> key.resourceKey().equals(resourceKey))
                .map(key -> findExactVersion(key.resourceKey(), key.semanticVersion()).orElseThrow())
                .toList();
    }

    @Override
    public synchronized List<ResourceCollection> findCollections() {
        return List.copyOf(collections.values());
    }

    @Override
    public synchronized Optional<ContentProvider> findProvider(String providerCode) {
        return Optional.ofNullable(providers.get(providerCode));
    }

    @Override
    public synchronized Optional<ResourceCollection> findCollection(String collectionKey) {
        return Optional.ofNullable(collections.get(collectionKey));
    }

    @Override
    public synchronized Optional<ResourceAsset> findAsset(String assetKey) {
        return Optional.ofNullable(assets.get(assetKey));
    }

    private static boolean isPublishedCandidate(
            LearningResource resource,
            ResourceCollection collection,
            ResourceCatalogEntry stored,
            ResourceCandidateQuery query
    ) {
        return resource.publishStatus() == PublishStatus.PUBLISHED
                && resource.accessScope() != AccessScope.DISABLED
                && resource.activeVersion() != null
                && resource.activeVersion().equals(stored.resourceVersion().semanticVersion())
                && stored.resourceVersion().status() == ResourceVersionStatus.PUBLISHED
                && collection.status() == CollectionStatus.ACTIVE
                && collection.accessScope() != AccessScope.DISABLED
                && (query.resourceType() == null || query.resourceType() == resource.type())
                && (query.collectionKey() == null || query.collectionKey().equals(resource.collectionKey()))
                && (query.level() == null || query.level() == resource.level())
                && (query.skillUnitVariantKey() == null
                    || stored.resourceVersion().skillUnitVariantKeys().contains(query.skillUnitVariantKey()))
                && (query.topic() == null || query.topic().equalsIgnoreCase(resource.topic()))
                && (query.scene() == null || query.scene().equalsIgnoreCase(resource.scene()))
                && (query.accessScope() == null || query.accessScope() == resource.accessScope());
    }

    private static PublishedResourceCandidate toCandidate(
            LearningResource resource,
            ResourceCatalogEntry entry,
            ResourceAsset taskHero,
            List<ResourceAsset> assets
    ) {
        return new PublishedResourceCandidate(
                resource.resourceKey(),
                entry.resourceVersion().semanticVersion(),
                resource.providerCode(),
                resource.collectionKey(),
                resource.type(),
                resource.title(),
                resource.level(),
                resource.topic(),
                resource.scene(),
                resource.communicationGoal(),
                resource.accessScope(),
                resource.estimatedMinutes(),
                entry.resourceVersion().skillUnitVariantKeys(),
                taskHero,
                assets);
    }

    private static ResourceVersionSaveResult result(
            ResourceCatalogEntry entry,
            CatalogWriteOutcome outcome
    ) {
        return new ResourceVersionSaveResult(
                entry.resource().resourceKey(),
                entry.resourceVersion().semanticVersion(),
                entry.resourceVersion().manifestHash(),
                outcome);
    }

    private static boolean sameImmutableAsset(ResourceAsset existing, ResourceAsset candidate) {
        return existing.assetKey().equals(candidate.assetKey())
                && existing.assetVersion().equals(candidate.assetVersion())
                && existing.mediaType() == candidate.mediaType()
                && existing.purpose() == candidate.purpose()
                && existing.objectKey().equals(candidate.objectKey())
                && existing.contentHash().equals(candidate.contentHash())
                && existing.mimeType().equals(candidate.mimeType())
                && existing.byteLength() == candidate.byteLength()
                && existing.accessScope() == candidate.accessScope()
                && existing.metadata().equals(candidate.metadata())
                && existing.status() == candidate.status()
                && existing.createdAt().toEpochMilli() == candidate.createdAt().toEpochMilli();
    }

    private record VersionKey(String resourceKey, String semanticVersion) {
        @Override
        public String toString() {
            return resourceKey + "@" + semanticVersion;
        }
    }
}
