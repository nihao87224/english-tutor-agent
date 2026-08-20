package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementApplicationException;
import cn.forever24.tutor.application.entitlement.ResourceAccessTarget;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.entitlement.EntitlementStatus;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class JdbcEntitlementRepository implements EntitlementStoreAdapter {

    private static final String ENTITLEMENT_SELECT = """
            SELECT e.entitlement_key, u.user_key, c.collection_key, e.status,
                   e.granted_by_user_id, e.granted_at_utc, e.expires_at_utc,
                   e.revoked_at_utc, e.reason, e.version
            FROM user_collection_entitlement e
            JOIN app_user u ON u.id = e.user_id
            JOIN resource_collection c ON c.id = e.collection_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcEntitlementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Entitlement> find(UserKey userKey, String collectionKey) {
        return jdbcTemplate.query(
                ENTITLEMENT_SELECT + " WHERE u.user_key = ? AND c.collection_key = ?",
                entitlementMapper(),
                userKey.value(),
                collectionKey).stream().findFirst();
    }

    @Override
    public Optional<Entitlement> findForUpdate(UserKey userKey, String collectionKey) {
        return jdbcTemplate.query(
                ENTITLEMENT_SELECT + " WHERE u.user_key = ? AND c.collection_key = ? FOR UPDATE",
                entitlementMapper(),
                userKey.value(),
                collectionKey).stream().findFirst();
    }

    @Override
    public List<Entitlement> findForUser(UserKey userKey) {
        return jdbcTemplate.query(
                ENTITLEMENT_SELECT + " WHERE u.user_key = ? ORDER BY c.collection_key",
                entitlementMapper(),
                userKey.value());
    }

    @Override
    public void insert(Entitlement entitlement) {
        int inserted = jdbcTemplate.update("""
                        INSERT INTO user_collection_entitlement (
                            entitlement_key, user_id, collection_id, status, granted_by_user_id,
                            granted_at_utc, expires_at_utc, revoked_at_utc, reason, version
                        )
                        SELECT ?, u.id, c.id, ?, ?, ?, ?, ?, ?, ?
                        FROM app_user u
                        JOIN resource_collection c ON c.collection_key = ?
                        WHERE u.user_key = ?
                        """,
                entitlement.entitlementKey(),
                entitlement.status().name(),
                entitlement.grantedByUserId(),
                Timestamp.from(entitlement.grantedAt()),
                timestamp(entitlement.expiresAt()),
                timestamp(entitlement.revokedAt()),
                entitlement.reason(),
                entitlement.version(),
                entitlement.collectionKey(),
                entitlement.userKey().value());
        if (inserted != 1) {
            throw EntitlementApplicationException.notFound(
                    "ENTITLEMENT_TARGET_NOT_FOUND", "user or collection was not found");
        }
    }

    @Override
    public void update(Entitlement entitlement, long expectedVersion) {
        int updated = jdbcTemplate.update("""
                        UPDATE user_collection_entitlement
                        SET status = ?, granted_by_user_id = ?, granted_at_utc = ?,
                            expires_at_utc = ?, revoked_at_utc = ?, reason = ?, version = ?
                        WHERE entitlement_key = ? AND version = ?
                        """,
                entitlement.status().name(),
                entitlement.grantedByUserId(),
                Timestamp.from(entitlement.grantedAt()),
                timestamp(entitlement.expiresAt()),
                timestamp(entitlement.revokedAt()),
                entitlement.reason(),
                entitlement.version(),
                entitlement.entitlementKey(),
                expectedVersion);
        if (updated != 1) {
            throw EntitlementApplicationException.conflict(
                    "ENTITLEMENT_VERSION_CONFLICT", "entitlement version changed");
        }
    }

    @Override
    public Optional<ResourceAccessTarget> findByResourceKey(String resourceKey) {
        return jdbcTemplate.query("""
                        SELECT r.resource_key, r.provider_code, r.resource_type, r.title AS resource_title,
                               r.description, r.language, r.level, r.topic, r.scene, r.communication_goal,
                               r.access_scope AS resource_access_scope, r.publish_status,
                               rv.semantic_version AS active_version, r.estimated_minutes,
                               c.collection_key, c.title AS collection_title,
                               c.access_scope AS collection_access_scope, c.status AS collection_status,
                               c.source_url, c.ownership_type, c.license_note,
                               c.allowed_audience, c.admin_note
                        FROM learning_resource r
                        JOIN resource_collection c ON c.id = r.collection_id
                        LEFT JOIN learning_resource_version rv ON rv.id = r.active_version_id
                        WHERE r.resource_key = ?
                        """,
                (rs, rowNum) -> accessTarget(rs),
                resourceKey).stream().findFirst();
    }

    @Override
    public boolean collectionExists(String collectionKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource_collection WHERE collection_key = ?",
                Integer.class,
                collectionKey);
        return count != null && count > 0;
    }

    private static RowMapper<Entitlement> entitlementMapper() {
        return (rs, rowNum) -> new Entitlement(
                rs.getString("entitlement_key"),
                new UserKey(rs.getString("user_key")),
                rs.getString("collection_key"),
                EntitlementStatus.valueOf(rs.getString("status")),
                rs.getLong("granted_by_user_id"),
                toInstant(rs.getTimestamp("granted_at_utc")),
                toInstant(rs.getTimestamp("expires_at_utc")),
                toInstant(rs.getTimestamp("revoked_at_utc")),
                rs.getString("reason"),
                rs.getLong("version"));
    }

    private static ResourceAccessTarget accessTarget(ResultSet rs) throws SQLException {
        String collectionKey = rs.getString("collection_key");
        LearningResource resource = new LearningResource(
                rs.getString("resource_key"),
                rs.getString("provider_code"),
                collectionKey,
                ResourceType.valueOf(rs.getString("resource_type")),
                rs.getString("resource_title"),
                rs.getString("description"),
                rs.getString("language"),
                CefrLevel.valueOf(rs.getString("level")),
                rs.getString("topic"),
                rs.getString("scene"),
                rs.getString("communication_goal"),
                AccessScope.valueOf(rs.getString("resource_access_scope")),
                PublishStatus.valueOf(rs.getString("publish_status")),
                rs.getString("active_version"),
                rs.getInt("estimated_minutes"));
        ResourceCollection collection = new ResourceCollection(
                collectionKey,
                rs.getString("provider_code"),
                rs.getString("collection_title"),
                AccessScope.valueOf(rs.getString("collection_access_scope")),
                CollectionStatus.valueOf(rs.getString("collection_status")),
                rs.getString("source_url"),
                rs.getString("ownership_type"),
                rs.getString("license_note"),
                rs.getString("allowed_audience"),
                rs.getString("admin_note"));
        return new ResourceAccessTarget(resource, collection);
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
