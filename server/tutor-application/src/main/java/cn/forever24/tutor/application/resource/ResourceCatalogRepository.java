package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceCatalogEntry;

import java.util.List;
import java.util.Optional;

public interface ResourceCatalogRepository
        extends ContentProviderRepository, CollectionRepository, AssetMetadataRepository {

    ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry);

    Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion);

    List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query);
}
