package cn.forever24.tutor.infrastructure.audio;

import cn.forever24.tutor.application.audio.AudioAssetRepository;
import cn.forever24.tutor.application.audio.AudioAssetStoreRecord;
import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;

public final class JdbcAudioAssetRepository implements AudioAssetRepository {
    private final JdbcTemplate jdbc;

    public JdbcAudioAssetRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<AudioAssetStoreRecord> findByIdempotencyKey(UserKey userKey, String idempotencyKey) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(select() + " AND a.idempotency_key = ?",
                    (rs, row) -> new AudioAssetStoreRecord(rs.getString("request_hash"), map(rs)),
                    userKey.value(), idempotencyKey));
        } catch (EmptyResultDataAccessException exception) { return Optional.empty(); }
    }

    @Override
    public Optional<UserAudioAsset> findById(UserKey userKey, String audioAssetId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(select() + " AND a.asset_key = ?",
                    (rs, row) -> map(rs), userKey.value(), audioAssetId));
        } catch (EmptyResultDataAccessException exception) { return Optional.empty(); }
    }

    @Override
    public void insert(UserKey userKey, UserAudioAsset asset, String idempotencyKey, String requestHash) {
        int changed = jdbc.update("""
                INSERT INTO user_audio_asset
                    (asset_key, user_id, object_key, idempotency_key, request_hash, purpose, mime_type,
                     byte_length, duration_ms, content_hash, status, retention_mode, delete_after_utc,
                     created_at_utc, updated_at_utc, version)
                SELECT ?, u.id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1
                FROM app_user u WHERE u.user_key = ? AND u.status = 'ACTIVE'
                """, asset.audioAssetId(), asset.objectKey(), idempotencyKey, requestHash, asset.purpose(),
                asset.mimeType(), asset.byteLength(), asset.durationMs(), asset.contentHash(), asset.status().name(),
                asset.retention().name(), utc(asset.deleteAfter()), utc(asset.createdAt()), utc(asset.createdAt()),
                userKey.value());
        if (changed != 1) throw new IllegalStateException("active audio asset owner was not found");
    }

    @Override
    public void markDeleted(UserKey userKey, String audioAssetId) {
        int changed = jdbc.update("""
                UPDATE user_audio_asset a JOIN app_user u ON u.id = a.user_id
                SET a.status = 'DELETED', a.updated_at_utc = UTC_TIMESTAMP(6), a.version = a.version + 1
                WHERE u.user_key = ? AND a.asset_key = ? AND a.status = 'READY'
                """, userKey.value(), audioAssetId);
        if (changed != 1) throw new IllegalStateException("ready audio asset was not found for deletion");
    }

    @Override
    public List<cn.forever24.tutor.application.audio.OwnedAudioAsset> findExpired(Instant now, int limit) {
        return jdbc.query("""
                SELECT a.*, u.user_key FROM user_audio_asset a JOIN app_user u ON u.id = a.user_id
                WHERE a.status = 'READY' AND a.delete_after_utc IS NOT NULL AND a.delete_after_utc <= ?
                ORDER BY a.delete_after_utc, a.id LIMIT ?
                """, (rs, row) -> new cn.forever24.tutor.application.audio.OwnedAudioAsset(
                        new UserKey(rs.getString("user_key")), map(rs)), utc(now), limit);
    }

    private String select() {
        return """
                SELECT a.* FROM user_audio_asset a JOIN app_user u ON u.id = a.user_id
                WHERE u.user_key = ? AND u.status = 'ACTIVE'
                """;
    }

    private UserAudioAsset map(ResultSet rs) throws SQLException {
        return new UserAudioAsset(rs.getString("asset_key"), rs.getString("object_key"), rs.getString("purpose"),
                rs.getString("mime_type"), rs.getLong("byte_length"), rs.getLong("duration_ms"),
                rs.getString("content_hash"), AudioAssetStatus.valueOf(rs.getString("status")),
                RawContentRetention.valueOf(rs.getString("retention_mode")), instant(rs.getTimestamp("delete_after_utc")),
                instant(rs.getTimestamp("created_at_utc")));
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
