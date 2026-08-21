package cn.forever24.tutor.application.content;

import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.content.ContentImportBatch;
import cn.forever24.tutor.content.ContentImportIssue;
import cn.forever24.tutor.content.ContentImportStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ContentImportApplicationService {
    private final ContentManifestValidator validator;
    private final ContentImportRepository batches;
    private final ResourceCatalogRepository catalog;
    private final Clock clock;

    public ContentImportApplicationService(ContentManifestValidator validator, ContentImportRepository batches,
                                           ResourceCatalogRepository catalog, Clock clock) {
        this.validator = Objects.requireNonNull(validator);
        this.batches = Objects.requireNonNull(batches);
        this.catalog = Objects.requireNonNull(catalog);
        this.clock = Objects.requireNonNull(clock);
    }

    public ContentImportBatch importManifest(String manifestJson) {
        String hash = sha256(manifestJson);
        return batches.findByContentHash(hash).orElseGet(() -> execute(hash, manifestJson));
    }

    private ContentImportBatch execute(String hash, String manifestJson) {
        Instant now = clock.instant();
        ContentManifestValidator.Validation validation = validator.validate(manifestJson);
        if (!validation.issues().isEmpty()) {
            return batches.save(new ContentImportBatch("imp_" + UUID.randomUUID().toString().replace("-", ""), hash,
                    ContentImportStatus.REJECTED, null, validation.issues(), now, now));
        }
        try {
            var entry = validation.entry().orElseThrow();
            catalog.saveExactVersion(entry);
            return batches.save(new ContentImportBatch("imp_" + UUID.randomUUID().toString().replace("-", ""), hash,
                    ContentImportStatus.IMPORTED_DRAFT, entry.resource().resourceKey(), List.of(), now, now));
        } catch (RuntimeException exception) {
            ContentImportIssue issue = new ContentImportIssue("CATALOG_WRITE_FAILED", "/", "validated content could not be imported");
            return batches.save(new ContentImportBatch("imp_" + UUID.randomUUID().toString().replace("-", ""), hash,
                    ContentImportStatus.IMPORT_FAILED, null, List.of(issue), now, now));
        }
    }

    private static String sha256(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("manifestJson is required");
        try { return "sha256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
}
