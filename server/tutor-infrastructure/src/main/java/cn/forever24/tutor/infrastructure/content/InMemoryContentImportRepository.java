package cn.forever24.tutor.infrastructure.content;

import cn.forever24.tutor.application.content.ContentImportRepository;
import cn.forever24.tutor.content.ContentImportBatch;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryContentImportRepository implements ContentImportRepository {
    private final Map<String, ContentImportBatch> batches = new ConcurrentHashMap<>();
    @Override public Optional<ContentImportBatch> findByContentHash(String contentHash) { return Optional.ofNullable(batches.get(contentHash)); }
    @Override public ContentImportBatch save(ContentImportBatch batch) { batches.putIfAbsent(batch.contentHash(), batch); return batches.get(batch.contentHash()); }
}
