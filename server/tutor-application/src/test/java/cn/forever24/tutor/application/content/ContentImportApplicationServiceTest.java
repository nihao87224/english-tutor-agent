package cn.forever24.tutor.application.content;

import cn.forever24.tutor.content.ContentImportIssue;
import cn.forever24.tutor.content.ContentImportStatus;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ContentImportApplicationServiceTest {
    @Test void rejectsInvalidManifestAndReplaysTheSameHashWithoutCatalogWrite() {
        ContentManifestValidator validator = ignored -> ContentManifestValidator.Validation.invalid(List.of(
                new ContentImportIssue("TASK_HERO_COUNT", "/assets", "exactly one task hero is required")));
        var catalog = mock(cn.forever24.tutor.application.resource.ResourceCatalogRepository.class);
        ContentImportApplicationService service = new ContentImportApplicationService(validator, new MemoryBatches(), catalog,
                Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC));
        var first = service.importManifest("{\"invalid\":true}");
        var replay = service.importManifest("{\"invalid\":true}");
        assertEquals(ContentImportStatus.REJECTED, first.status());
        assertEquals(first.batchKey(), replay.batchKey());
        assertEquals("TASK_HERO_COUNT", first.issues().getFirst().code());
        verifyNoInteractions(catalog);
    }
    private static final class MemoryBatches implements ContentImportRepository {
        private final java.util.Map<String, cn.forever24.tutor.content.ContentImportBatch> values = new ConcurrentHashMap<>();
        @Override public Optional<cn.forever24.tutor.content.ContentImportBatch> findByContentHash(String hash) { return Optional.ofNullable(values.get(hash)); }
        @Override public cn.forever24.tutor.content.ContentImportBatch save(cn.forever24.tutor.content.ContentImportBatch batch) { values.putIfAbsent(batch.contentHash(), batch); return values.get(batch.contentHash()); }
    }
}
