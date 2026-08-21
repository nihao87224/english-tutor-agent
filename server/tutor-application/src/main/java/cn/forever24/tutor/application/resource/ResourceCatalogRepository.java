package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;

import java.util.List;
import java.util.Optional;

public interface ResourceCatalogRepository
        extends ContentProviderRepository, CollectionRepository, AssetMetadataRepository {

    ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry);

    Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion);

    List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query);

    List<ResourceCatalogEntry> findAllResourceVersions();

    List<ResourceCatalogEntry> findResourceVersions(String resourceKey);

    List<ResourceCollection> findCollections();

    /** Updates only mutable publication fields; manifest and asset data stay immutable. */
    default void replacePublicationState(ResourceCatalogEntry entry) {
        throw new UnsupportedOperationException("catalog publication state updates are not supported");
    }

    default void replaceCollection(ResourceCollection collection) {
        throw new UnsupportedOperationException("collection state updates are not supported");
    }
}
