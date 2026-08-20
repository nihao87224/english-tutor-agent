package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceCatalogEntry;

import java.util.List;
import java.util.Optional;

public class ResourceCatalogApplicationService {

    private final ResourceCatalogRepository repository;

    public ResourceCatalogApplicationService(ResourceCatalogRepository repository) {
        this.repository = repository;
    }

    public ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("catalog entry is required");
        }
        return repository.saveExactVersion(entry);
    }

    public Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion) {
        if (resourceKey == null || resourceKey.isBlank() || semanticVersion == null || semanticVersion.isBlank()) {
            throw new IllegalArgumentException("resource key and semantic version are required");
        }
        return repository.findExactVersion(resourceKey, semanticVersion);
    }

    public List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query) {
        return repository.findPublishedCandidates(query == null ? ResourceCandidateQuery.allPublished() : query);
    }
}
