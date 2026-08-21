package cn.forever24.tutor.application.content;

import cn.forever24.tutor.content.ContentImportBatch;

import java.util.Optional;

public interface ContentImportRepository {
    Optional<ContentImportBatch> findByContentHash(String contentHash);
    ContentImportBatch save(ContentImportBatch batch);
}
