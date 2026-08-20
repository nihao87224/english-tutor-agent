package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogWriteOutcome;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.application.resource.ResourceCatalogConflictException;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.PublishStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryResourceCatalogRepositoryTest {

    @Test
    void sameVersionAndHashIsIdempotentButDifferentHashConflicts() {
        InMemoryResourceCatalogRepository repository = new InMemoryResourceCatalogRepository();

        assertEquals(CatalogWriteOutcome.CREATED,
                repository.saveExactVersion(ResourceCatalogTestFixture.publishedEntry()).outcome());
        assertEquals(CatalogWriteOutcome.ALREADY_EXISTS,
                repository.saveExactVersion(ResourceCatalogTestFixture.publishedEntry()).outcome());
        assertThrows(ResourceCatalogConflictException.class, () -> repository.saveExactVersion(
                ResourceCatalogTestFixture.entry(
                        "", 'd', PublishStatus.PUBLISHED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE)));
    }

    @Test
    void projectsOnlyPublishedMatchingCandidateMetadata() {
        InMemoryResourceCatalogRepository repository = new InMemoryResourceCatalogRepository();
        repository.saveExactVersion(ResourceCatalogTestFixture.publishedEntry());

        var candidates = repository.findPublishedCandidates(new ResourceCandidateQuery(
                CefrLevel.B1,
                "travel.confirm_gate_change.b1",
                "travel",
                "gate_change",
                null));

        assertEquals(1, candidates.size());
        assertEquals("1.0.0", candidates.getFirst().semanticVersion());
        assertEquals("season1.ep006.gate_change.b1.task-hero", candidates.getFirst().taskHero().assetKey());
        assertEquals(2, candidates.getFirst().assets().size());
        assertTrue(repository.findAsset(candidates.getFirst().taskHero().assetKey()).isPresent());
    }

    @Test
    void disabledResourceRemainsReadableByExactVersionButIsNotCandidate() {
        InMemoryResourceCatalogRepository repository = new InMemoryResourceCatalogRepository();
        var disabled = ResourceCatalogTestFixture.entry(
                ".disabled", 'e', PublishStatus.DISABLED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE);
        repository.saveExactVersion(disabled);

        assertTrue(repository.findExactVersion(
                disabled.resource().resourceKey(), disabled.resourceVersion().semanticVersion()).isPresent());
        assertTrue(repository.findPublishedCandidates(ResourceCandidateQuery.allPublished()).isEmpty());
    }
}
