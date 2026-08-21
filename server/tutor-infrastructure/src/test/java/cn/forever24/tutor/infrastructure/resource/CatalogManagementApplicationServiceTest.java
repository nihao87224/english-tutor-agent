package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogApplicationException;
import cn.forever24.tutor.application.resource.CatalogManagementApplicationService;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.PublishStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogManagementApplicationServiceTest {

    @Test
    void publishesExactDraftOnlyWhenImportedManifestHashMatches() {
        InMemoryResourceCatalogRepository repository = new InMemoryResourceCatalogRepository();
        var draft = ResourceCatalogTestFixture.entry("", 'a', PublishStatus.DRAFT, CollectionStatus.ACTIVE,
                cn.forever24.tutor.resource.AssetStatus.ACTIVE);
        repository.saveExactVersion(draft);
        CatalogManagementApplicationService service = new CatalogManagementApplicationService(
                repository, (actor, action, target, at) -> { },
                Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC));

        assertThrows(CatalogApplicationException.class, () -> service.publish(1, draft.resource().resourceKey(),
                "1.0.0", "sha256:" + "b".repeat(64)));

        var published = service.publish(1, draft.resource().resourceKey(), "1.0.0", draft.resourceVersion().manifestHash());

        assertEquals(PublishStatus.PUBLISHED, published.resource().publishStatus());
        assertEquals("1.0.0", published.resource().activeVersion());
        assertEquals(1, repository.findPublishedCandidates(ResourceCandidateQuery.allPublished()).size());
    }
}
