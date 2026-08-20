package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ContentProvider;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceCatalogApplicationServiceTest {

    @Test
    void defaultsNullCandidateQueryToAllPublished() {
        RecordingRepository repository = new RecordingRepository();
        ResourceCatalogApplicationService service = new ResourceCatalogApplicationService(repository);

        assertEquals(List.of(), service.findPublishedCandidates(null));
        assertEquals(ResourceCandidateQuery.allPublished(), repository.lastQuery);
    }

    @Test
    void rejectsIncompleteExactVersionLookup() {
        ResourceCatalogApplicationService service = new ResourceCatalogApplicationService(new RecordingRepository());

        assertThrows(IllegalArgumentException.class, () -> service.findExactVersion("resource", ""));
        assertThrows(IllegalArgumentException.class, () -> service.saveExactVersion(null));
    }

    private static final class RecordingRepository implements ResourceCatalogRepository {

        private ResourceCandidateQuery lastQuery;

        @Override
        public ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion) {
            return Optional.empty();
        }

        @Override
        public List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query) {
            lastQuery = query;
            return List.of();
        }

        @Override
        public Optional<ContentProvider> findProvider(String providerCode) {
            return Optional.empty();
        }

        @Override
        public Optional<ResourceCollection> findCollection(String collectionKey) {
            return Optional.empty();
        }

        @Override
        public Optional<ResourceAsset> findAsset(String assetKey) {
            return Optional.empty();
        }
    }
}
