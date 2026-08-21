package cn.forever24.tutor.infrastructure.content;

import cn.forever24.tutor.application.content.ContentImportRepository;
import cn.forever24.tutor.content.ContentImportBatch;
import cn.forever24.tutor.content.ContentImportIssue;
import cn.forever24.tutor.content.ContentImportStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcContentImportRepository implements ContentImportRepository {
    private final JdbcTemplate jdbc;
    public JdbcContentImportRepository(JdbcTemplate jdbc) { this.jdbc = Objects.requireNonNull(jdbc); }
    @Override public Optional<ContentImportBatch> findByContentHash(String hash) {
        try {
            Batch row = jdbc.queryForObject("SELECT id, batch_key, content_hash, status, resource_key, created_at_utc, completed_at_utc FROM content_import_batch WHERE content_hash = ?",
                    (rs, ignored) -> new Batch(rs.getLong("id"), rs.getString("batch_key"), rs.getString("content_hash"), ContentImportStatus.valueOf(rs.getString("status")), rs.getString("resource_key"), rs.getTimestamp("created_at_utc").toInstant(), rs.getTimestamp("completed_at_utc") == null ? null : rs.getTimestamp("completed_at_utc").toInstant()), hash);
            return Optional.of(new ContentImportBatch(row.key(), row.hash(), row.status(), row.resourceKey(), issues(row.id()), row.createdAt(), row.completedAt()));
        } catch (EmptyResultDataAccessException ignored) { return Optional.empty(); }
    }
    @Override public ContentImportBatch save(ContentImportBatch batch) {
        Optional<ContentImportBatch> existing = findByContentHash(batch.contentHash()); if (existing.isPresent()) return existing.orElseThrow();
        KeyHolder keys = new GeneratedKeyHolder(); LocalDateTime created = LocalDateTime.ofInstant(batch.createdAt(), ZoneOffset.UTC); LocalDateTime completed = batch.completedAt() == null ? null : LocalDateTime.ofInstant(batch.completedAt(), ZoneOffset.UTC);
        jdbc.update(connection -> { PreparedStatement ps = connection.prepareStatement("INSERT INTO content_import_batch (batch_key, content_hash, status, resource_key, created_at_utc, completed_at_utc, version) VALUES (?, ?, ?, ?, ?, ?, 1)", Statement.RETURN_GENERATED_KEYS); ps.setString(1, batch.batchKey()); ps.setString(2, batch.contentHash()); ps.setString(3, batch.status().name()); ps.setString(4, batch.resourceKey()); ps.setObject(5, created); ps.setObject(6, completed); return ps; }, keys);
        Number id = keys.getKey(); if (id == null) throw new IllegalStateException("content import batch id was not generated");
        for (int index = 0; index < batch.issues().size(); index++) { ContentImportIssue issue = batch.issues().get(index); jdbc.update("INSERT INTO content_import_issue (batch_id, issue_code, location, message, sequence_no) VALUES (?, ?, ?, ?, ?)", id.longValue(), issue.code(), issue.location(), issue.message(), index); }
        return batch;
    }
    private List<ContentImportIssue> issues(long id) { return jdbc.query("SELECT issue_code, location, message FROM content_import_issue WHERE batch_id = ? ORDER BY sequence_no", (rs, ignored) -> new ContentImportIssue(rs.getString(1), rs.getString(2), rs.getString(3)), id); }
    private record Batch(long id, String key, String hash, ContentImportStatus status, String resourceKey, java.time.Instant createdAt, java.time.Instant completedAt) { }
}
