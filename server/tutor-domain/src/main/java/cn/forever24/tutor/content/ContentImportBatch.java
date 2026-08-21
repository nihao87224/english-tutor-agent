package cn.forever24.tutor.content;

import java.time.Instant;
import java.util.List;

public record ContentImportBatch(
        String batchKey, String contentHash, ContentImportStatus status, String resourceKey,
        List<ContentImportIssue> issues, Instant createdAt, Instant completedAt
) {
    public ContentImportBatch {
        if (batchKey == null || batchKey.isBlank() || contentHash == null || contentHash.isBlank()
                || status == null || issues == null || createdAt == null) {
            throw new IllegalArgumentException("content import batch fields are required");
        }
        batchKey = batchKey.strip();
        contentHash = contentHash.strip();
        resourceKey = resourceKey == null || resourceKey.isBlank() ? null : resourceKey.strip();
        issues = List.copyOf(issues);
    }
}
