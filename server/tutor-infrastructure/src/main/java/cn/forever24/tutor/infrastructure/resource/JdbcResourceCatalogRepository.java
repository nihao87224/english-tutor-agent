package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogWriteOutcome;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.application.resource.ResourceCatalogConflictException;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.application.resource.ResourceVersionSaveResult;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetMediaType;
import cn.forever24.tutor.resource.AssetMetadata;
import cn.forever24.tutor.resource.AssetPurpose;
import cn.forever24.tutor.resource.AssetReference;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.AudioAssetMetadata;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.ContentProvider;
import cn.forever24.tutor.resource.ContentProviderType;
import cn.forever24.tutor.resource.ImageAssetMetadata;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import cn.forever24.tutor.resource.ResourceVersion;
import cn.forever24.tutor.resource.ResourceVersionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JdbcResourceCatalogRepository implements ResourceCatalogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcResourceCatalogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("catalog entry is required");
        }
        Optional<String> existingHash = findManifestHash(
                entry.resource().resourceKey(), entry.resourceVersion().semanticVersion());
        if (existingHash.isPresent()) {
            if (existingHash.get().equals(entry.resourceVersion().manifestHash())) {
                return result(entry, CatalogWriteOutcome.ALREADY_EXISTS);
            }
            throw new ResourceCatalogConflictException(
                    "resource version already exists with a different manifest hash: "
                            + entry.resource().resourceKey() + "@" + entry.resourceVersion().semanticVersion());
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        upsertProvider(entry.provider());
        long collectionId = upsertCollection(entry.collection(), now);
        long resourceId = upsertResource(entry.resource(), collectionId, now);
        List<Long> assetIds = new ArrayList<>();
        for (ResourceAsset asset : entry.assets()) {
            assetIds.add(persistAsset(asset));
        }
        long resourceVersionId = insertVersion(resourceId, entry.resourceVersion());
        persistAssetReferences(resourceVersionId, entry.resourceVersion().assetReferences(), entry.assets(), assetIds);
        persistSkillVariantReferences(resourceVersionId, entry.resourceVersion().skillUnitVariantKeys());
        upsertCollectionResource(collectionId, resourceId);
        if (entry.resourceVersion().semanticVersion().equals(entry.resource().activeVersion())) {
            jdbcTemplate.update(
                    "UPDATE learning_resource SET active_version_id = ?, updated_at_utc = ?, version = version + 1 WHERE id = ?",
                    resourceVersionId,
                    now,
                    resourceId);
        }
        return result(entry, CatalogWriteOutcome.CREATED);
    }

    @Override
    public Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion) {
        return jdbcTemplate.query("""
                        SELECT r.resource_key, r.resource_type, r.title, r.description, r.language,
                               r.level, r.topic, r.scene, r.communication_goal, r.access_scope,
                               r.publish_status, r.estimated_minutes,
                               p.provider_code, p.display_name, p.provider_type,
                               c.collection_key, c.title AS collection_title,
                               c.access_scope AS collection_access_scope, c.status AS collection_status,
                               c.source_url, c.ownership_type, c.license_note, c.allowed_audience, c.admin_note,
                               rv.id AS resource_version_id, rv.semantic_version, rv.manifest_hash,
                               rv.manifest_json, rv.learner_fit_json, rv.generation_metadata_json,
                               rv.created_at_utc, rv.published_at_utc, rv.status AS resource_version_status,
                               active_version.semantic_version AS active_semantic_version
                        FROM learning_resource r
                        JOIN content_provider p ON p.provider_code = r.provider_code
                        JOIN resource_collection c ON c.id = r.collection_id
                        JOIN learning_resource_version rv ON rv.resource_id = r.id
                        LEFT JOIN learning_resource_version active_version ON active_version.id = r.active_version_id
                        WHERE r.resource_key = ? AND rv.semantic_version = ?
                        """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    long versionId = resultSet.getLong("resource_version_id");
                    ContentProvider provider = new ContentProvider(
                            resultSet.getString("provider_code"),
                            resultSet.getString("display_name"),
                            ContentProviderType.valueOf(resultSet.getString("provider_type")));
                    ResourceCollection collection = mapCollection(resultSet);
                    LearningResource resource = mapResource(resultSet);
                    List<ResourceAsset> assets = loadAssets(versionId);
                    ResourceVersion version = new ResourceVersion(
                            resource.resourceKey(),
                            resultSet.getString("semantic_version"),
                            resultSet.getString("manifest_hash"),
                            resultSet.getString("manifest_json"),
                            resultSet.getString("learner_fit_json"),
                            resultSet.getString("generation_metadata_json"),
                            loadSkillVariantKeys(versionId),
                            loadAssetReferences(versionId),
                            ResourceVersionStatus.valueOf(resultSet.getString("resource_version_status")),
                            toInstant(resultSet.getTimestamp("created_at_utc")),
                            toNullableInstant(resultSet.getTimestamp("published_at_utc")));
                    return Optional.of(new ResourceCatalogEntry(provider, collection, resource, version, assets));
                },
                resourceKey,
                semanticVersion);
    }

    @Override
    public List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.resource_key, rv.semantic_version
                FROM learning_resource r
                JOIN learning_resource_version rv ON rv.id = r.active_version_id
                JOIN resource_collection c ON c.id = r.collection_id
                WHERE r.publish_status = 'PUBLISHED'
                  AND r.access_scope <> 'DISABLED'
                  AND rv.status = 'PUBLISHED'
                  AND c.status = 'ACTIVE'
                  AND c.access_scope <> 'DISABLED'
                  AND NOT EXISTS (
                    SELECT 1
                    FROM resource_version_asset rva
                    JOIN learning_asset blocked_asset ON blocked_asset.id = rva.asset_id
                    WHERE rva.resource_version_id = rv.id
                      AND (blocked_asset.status <> 'ACTIVE' OR blocked_asset.access_scope = 'DISABLED')
                  )
                """);
        List<Object> arguments = new ArrayList<>();
        if (query.resourceType() != null) {
            sql.append(" AND r.resource_type = ?");
            arguments.add(query.resourceType().name());
        }
        if (query.collectionKey() != null) {
            sql.append(" AND c.collection_key = ?");
            arguments.add(query.collectionKey());
        }
        if (query.level() != null) {
            sql.append(" AND r.level = ?");
            arguments.add(query.level().name());
        }
        if (query.skillUnitVariantKey() != null) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1
                       FROM resource_version_skill_variant rvsv
                       JOIN curriculum_skill_unit_variant variant ON variant.id = rvsv.variant_id
                       WHERE rvsv.resource_version_id = rv.id AND variant.variant_key = ?
                     )
                    """);
            arguments.add(query.skillUnitVariantKey());
        }
        if (query.topic() != null) {
            sql.append(" AND LOWER(r.topic) = LOWER(?)");
            arguments.add(query.topic());
        }
        if (query.scene() != null) {
            sql.append(" AND LOWER(r.scene) = LOWER(?)");
            arguments.add(query.scene());
        }
        if (query.accessScope() != null) {
            sql.append(" AND r.access_scope = ?");
            arguments.add(query.accessScope().name());
        }
        sql.append(" ORDER BY r.resource_key");

        return jdbcTemplate.query(
                        sql.toString(),
                        (resultSet, rowNumber) -> new VersionKey(
                                resultSet.getString("resource_key"),
                                resultSet.getString("semantic_version")),
                        arguments.toArray()).stream()
                .map(key -> findExactVersion(key.resourceKey(), key.semanticVersion()).orElseThrow())
                .map(JdbcResourceCatalogRepository::toCandidate)
                .toList();
    }

    @Override
    public List<ResourceCatalogEntry> findAllResourceVersions() {
        return jdbcTemplate.query("""
                        SELECT r.resource_key, rv.semantic_version
                        FROM learning_resource r
                        JOIN learning_resource_version rv ON rv.resource_id = r.id
                        ORDER BY r.resource_key, rv.created_at_utc DESC
                        """,
                (resultSet, rowNumber) -> new VersionKey(
                        resultSet.getString("resource_key"), resultSet.getString("semantic_version")))
                .stream()
                .map(key -> findExactVersion(key.resourceKey(), key.semanticVersion()).orElseThrow())
                .toList();
    }

    @Override
    public List<ResourceCatalogEntry> findResourceVersions(String resourceKey) {
        return jdbcTemplate.query("""
                        SELECT r.resource_key, rv.semantic_version
                        FROM learning_resource r
                        JOIN learning_resource_version rv ON rv.resource_id = r.id
                        WHERE r.resource_key = ?
                        ORDER BY rv.created_at_utc DESC
                        """,
                (resultSet, rowNumber) -> new VersionKey(
                        resultSet.getString("resource_key"), resultSet.getString("semantic_version")),
                resourceKey).stream()
                .map(key -> findExactVersion(key.resourceKey(), key.semanticVersion()).orElseThrow())
                .toList();
    }

    @Override
    public List<ResourceCollection> findCollections() {
        return jdbcTemplate.query("""
                        SELECT collection_key, provider_code, title AS collection_title,
                               access_scope AS collection_access_scope, status AS collection_status,
                               source_url, ownership_type, license_note, allowed_audience, admin_note
                        FROM resource_collection
                        ORDER BY collection_key
                        """,
                (resultSet, rowNumber) -> mapCollection(resultSet));
    }

    @Override
    @Transactional
    public void replacePublicationState(ResourceCatalogEntry entry) {
        Long resourceId = queryId("SELECT id FROM learning_resource WHERE resource_key = ?", entry.resource().resourceKey())
                .orElseThrow(() -> new ResourceCatalogConflictException("resource does not exist"));
        Long versionId = queryId("SELECT id FROM learning_resource_version WHERE resource_id = ? AND semantic_version = ?",
                resourceId, entry.resourceVersion().semanticVersion())
                .orElseThrow(() -> new ResourceCatalogConflictException("resource version does not exist"));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("UPDATE learning_resource_version SET status = ?, published_at_utc = ?, version = version + 1 WHERE id = ?",
                entry.resourceVersion().status().name(),
                entry.resourceVersion().publishedAt() == null ? null : Timestamp.from(entry.resourceVersion().publishedAt()), versionId);
        jdbcTemplate.update("""
                        UPDATE learning_resource
                        SET access_scope = ?, publish_status = ?, active_version_id = ?, updated_at_utc = ?, version = version + 1
                        WHERE id = ?
                        """,
                entry.resource().accessScope().name(), entry.resource().publishStatus().name(),
                entry.resource().activeVersion() == null ? null : versionId, now, resourceId);
    }

    @Override
    @Transactional
    public void replaceCollection(ResourceCollection collection) {
        int updated = jdbcTemplate.update("""
                        UPDATE resource_collection
                        SET access_scope = ?, status = ?, updated_at_utc = ?, version = version + 1
                        WHERE collection_key = ?
                        """, collection.accessScope().name(), collection.status().name(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), collection.collectionKey());
        if (updated != 1) {
            throw new ResourceCatalogConflictException("collection does not exist: " + collection.collectionKey());
        }
    }

    @Override
    public Optional<ContentProvider> findProvider(String providerCode) {
        return jdbcTemplate.query(
                "SELECT provider_code, display_name, provider_type FROM content_provider WHERE provider_code = ?",
                resultSet -> resultSet.next()
                        ? Optional.of(new ContentProvider(
                                resultSet.getString("provider_code"),
                                resultSet.getString("display_name"),
                                ContentProviderType.valueOf(resultSet.getString("provider_type"))))
                        : Optional.empty(),
                providerCode);
    }

    @Override
    public Optional<ResourceCollection> findCollection(String collectionKey) {
        return jdbcTemplate.query("""
                        SELECT collection_key, provider_code, title AS collection_title,
                               access_scope AS collection_access_scope, status AS collection_status,
                               source_url, ownership_type, license_note, allowed_audience, admin_note
                        FROM resource_collection WHERE collection_key = ?
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(mapCollection(resultSet))
                        : Optional.empty(),
                collectionKey);
    }

    @Override
    public Optional<ResourceAsset> findAsset(String assetKey) {
        return jdbcTemplate.query("SELECT * FROM learning_asset WHERE asset_key = ?",
                resultSet -> resultSet.next() ? Optional.of(mapAsset(resultSet)) : Optional.empty(),
                assetKey);
    }

    private Optional<String> findManifestHash(String resourceKey, String semanticVersion) {
        return jdbcTemplate.query("""
                        SELECT rv.manifest_hash
                        FROM learning_resource_version rv
                        JOIN learning_resource r ON r.id = rv.resource_id
                        WHERE r.resource_key = ? AND rv.semantic_version = ?
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getString("manifest_hash"))
                        : Optional.empty(),
                resourceKey,
                semanticVersion);
    }

    private void upsertProvider(ContentProvider provider) {
        int updated = jdbcTemplate.update(
                "UPDATE content_provider SET display_name = ?, provider_type = ? WHERE provider_code = ?",
                provider.displayName(),
                provider.type().name(),
                provider.providerCode());
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO content_provider (provider_code, display_name, provider_type) VALUES (?, ?, ?)",
                    provider.providerCode(),
                    provider.displayName(),
                    provider.type().name());
        }
    }

    private long upsertCollection(ResourceCollection collection, LocalDateTime now) {
        Optional<Long> existingId = queryId(
                "SELECT id FROM resource_collection WHERE collection_key = ?", collection.collectionKey());
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                            UPDATE resource_collection
                            SET provider_code = ?, title = ?, access_scope = ?, status = ?, source_url = ?,
                                ownership_type = ?, license_note = ?, allowed_audience = ?, admin_note = ?,
                                updated_at_utc = ?, version = version + 1
                            WHERE id = ?
                            """,
                    collection.providerCode(), collection.title(), collection.accessScope().name(),
                    collection.status().name(), collection.sourceUrl(), collection.ownershipType(),
                    collection.licenseNote(), collection.allowedAudience(), collection.adminNote(),
                    now, existingId.get());
            return existingId.get();
        }
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO resource_collection (
                        collection_key, provider_code, title, access_scope, status, source_url,
                        ownership_type, license_note, allowed_audience, admin_note,
                        created_at_utc, updated_at_utc, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, collection.collectionKey());
            statement.setString(2, collection.providerCode());
            statement.setString(3, collection.title());
            statement.setString(4, collection.accessScope().name());
            statement.setString(5, collection.status().name());
            statement.setString(6, collection.sourceUrl());
            statement.setString(7, collection.ownershipType());
            statement.setString(8, collection.licenseNote());
            statement.setString(9, collection.allowedAudience());
            statement.setString(10, collection.adminNote());
            statement.setTimestamp(11, Timestamp.valueOf(now));
            statement.setTimestamp(12, Timestamp.valueOf(now));
            return statement;
        });
    }

    private long upsertResource(LearningResource resource, long collectionId, LocalDateTime now) {
        Optional<Long> existingId = queryId(
                "SELECT id FROM learning_resource WHERE resource_key = ?", resource.resourceKey());
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                            UPDATE learning_resource
                            SET provider_code = ?, collection_id = ?, resource_type = ?, title = ?, description = ?,
                                language = ?, level = ?, topic = ?, scene = ?, communication_goal = ?,
                                access_scope = ?, publish_status = ?, estimated_minutes = ?,
                                updated_at_utc = ?, version = version + 1
                            WHERE id = ?
                            """,
                    resource.providerCode(), collectionId, resource.type().name(), resource.title(),
                    resource.description(), resource.language(), resource.level().name(), resource.topic(),
                    resource.scene(), resource.communicationGoal(), resource.accessScope().name(),
                    resource.publishStatus().name(), resource.estimatedMinutes(), now, existingId.get());
            return existingId.get();
        }
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO learning_resource (
                        resource_key, provider_code, collection_id, resource_type, title, description,
                        language, level, topic, scene, communication_goal, access_scope, publish_status,
                        active_version_id, estimated_minutes, created_at_utc, updated_at_utc, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, resource.resourceKey());
            statement.setString(2, resource.providerCode());
            statement.setLong(3, collectionId);
            statement.setString(4, resource.type().name());
            statement.setString(5, resource.title());
            statement.setString(6, resource.description());
            statement.setString(7, resource.language());
            statement.setString(8, resource.level().name());
            statement.setString(9, resource.topic());
            statement.setString(10, resource.scene());
            statement.setString(11, resource.communicationGoal());
            statement.setString(12, resource.accessScope().name());
            statement.setString(13, resource.publishStatus().name());
            statement.setInt(14, resource.estimatedMinutes());
            statement.setTimestamp(15, Timestamp.valueOf(now));
            statement.setTimestamp(16, Timestamp.valueOf(now));
            return statement;
        });
    }

    private long persistAsset(ResourceAsset asset) {
        Optional<ResourceAsset> existing = findAsset(asset.assetKey());
        if (existing.isPresent()) {
            if (sameImmutableAsset(existing.get(), asset)) {
                return queryId("SELECT id FROM learning_asset WHERE asset_key = ?", asset.assetKey()).orElseThrow();
            }
            throw new ResourceCatalogConflictException(
                    "asset key already exists with different immutable metadata: " + asset.assetKey());
        }
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO learning_asset (
                        asset_key, asset_version, media_type, purpose, object_key, content_hash,
                        mime_type, byte_length, access_scope, metadata_json, status, created_at_utc
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, asset.assetKey());
            statement.setString(2, asset.assetVersion());
            statement.setString(3, asset.mediaType().name());
            statement.setString(4, asset.purpose().name());
            statement.setString(5, asset.objectKey());
            statement.setString(6, asset.contentHash());
            statement.setString(7, asset.mimeType());
            statement.setLong(8, asset.byteLength());
            statement.setString(9, asset.accessScope().name());
            statement.setString(10, writeJson(asset.metadata()));
            statement.setString(11, asset.status().name());
            statement.setTimestamp(12, Timestamp.from(asset.createdAt()));
            return statement;
        });
    }

    private long insertVersion(long resourceId, ResourceVersion version) {
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO learning_resource_version (
                        resource_id, semantic_version, manifest_hash, manifest_json, learner_fit_json,
                        generation_metadata_json, created_at_utc, published_at_utc, status, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, resourceId);
            statement.setString(2, version.semanticVersion());
            statement.setString(3, version.manifestHash());
            statement.setString(4, version.manifestJson());
            statement.setString(5, version.learnerFitJson());
            statement.setString(6, version.generationMetadataJson());
            statement.setTimestamp(7, Timestamp.from(version.createdAt()));
            statement.setTimestamp(8, version.publishedAt() == null ? null : Timestamp.from(version.publishedAt()));
            statement.setString(9, version.status().name());
            return statement;
        });
    }

    private void persistAssetReferences(
            long versionId,
            List<AssetReference> references,
            List<ResourceAsset> assets,
            List<Long> assetIds
    ) {
        for (AssetReference reference : references) {
            int assetIndex = -1;
            for (int index = 0; index < assets.size(); index++) {
                if (assets.get(index).assetKey().equals(reference.assetKey())) {
                    assetIndex = index;
                    break;
                }
            }
            if (assetIndex < 0) {
                throw new IllegalArgumentException("unknown asset reference: " + reference.assetKey());
            }
            jdbcTemplate.update(
                    "INSERT INTO resource_version_asset (resource_version_id, asset_id, display_order) VALUES (?, ?, ?)",
                    versionId,
                    assetIds.get(assetIndex),
                    reference.displayOrder());
        }
    }

    private void persistSkillVariantReferences(long versionId, Set<String> variantKeys) {
        for (String variantKey : variantKeys) {
            long variantId = queryId(
                    "SELECT id FROM curriculum_skill_unit_variant WHERE variant_key = ?", variantKey)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "unknown Skill Unit Variant reference: " + variantKey));
            jdbcTemplate.update(
                    "INSERT INTO resource_version_skill_variant (resource_version_id, variant_id) VALUES (?, ?)",
                    versionId,
                    variantId);
        }
    }

    private void upsertCollectionResource(long collectionId, long resourceId) {
        int updated = jdbcTemplate.update("""
                        UPDATE collection_resource
                        SET display_order = 0, status = 'ACTIVE'
                        WHERE collection_id = ? AND resource_id = ?
                        """,
                collectionId,
                resourceId);
        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO collection_resource (collection_id, resource_id, display_order, status)
                            VALUES (?, ?, 0, 'ACTIVE')
                            """,
                    collectionId,
                    resourceId);
        }
    }

    private List<ResourceAsset> loadAssets(long versionId) {
        return jdbcTemplate.query("""
                        SELECT a.*
                        FROM resource_version_asset rva
                        JOIN learning_asset a ON a.id = rva.asset_id
                        WHERE rva.resource_version_id = ?
                        ORDER BY rva.display_order
                        """,
                (resultSet, rowNumber) -> mapAsset(resultSet),
                versionId);
    }

    private List<AssetReference> loadAssetReferences(long versionId) {
        return jdbcTemplate.query("""
                        SELECT a.asset_key, rva.display_order
                        FROM resource_version_asset rva
                        JOIN learning_asset a ON a.id = rva.asset_id
                        WHERE rva.resource_version_id = ?
                        ORDER BY rva.display_order
                        """,
                (resultSet, rowNumber) -> new AssetReference(
                        resultSet.getString("asset_key"), resultSet.getInt("display_order")),
                versionId);
    }

    private Set<String> loadSkillVariantKeys(long versionId) {
        return Set.copyOf(jdbcTemplate.query("""
                        SELECT variant.variant_key
                        FROM resource_version_skill_variant reference
                        JOIN curriculum_skill_unit_variant variant ON variant.id = reference.variant_id
                        WHERE reference.resource_version_id = ?
                        ORDER BY variant.variant_key
                        """,
                (resultSet, rowNumber) -> resultSet.getString("variant_key"),
                versionId));
    }

    private ResourceCollection mapCollection(ResultSet resultSet) throws SQLException {
        return new ResourceCollection(
                resultSet.getString("collection_key"),
                resultSet.getString("provider_code"),
                resultSet.getString("collection_title"),
                AccessScope.valueOf(resultSet.getString("collection_access_scope")),
                CollectionStatus.valueOf(resultSet.getString("collection_status")),
                resultSet.getString("source_url"),
                resultSet.getString("ownership_type"),
                resultSet.getString("license_note"),
                resultSet.getString("allowed_audience"),
                resultSet.getString("admin_note"));
    }

    private LearningResource mapResource(ResultSet resultSet) throws SQLException {
        return new LearningResource(
                resultSet.getString("resource_key"),
                resultSet.getString("provider_code"),
                resultSet.getString("collection_key"),
                ResourceType.valueOf(resultSet.getString("resource_type")),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("language"),
                CefrLevel.valueOf(resultSet.getString("level")),
                resultSet.getString("topic"),
                resultSet.getString("scene"),
                resultSet.getString("communication_goal"),
                AccessScope.valueOf(resultSet.getString("access_scope")),
                PublishStatus.valueOf(resultSet.getString("publish_status")),
                resultSet.getString("active_semantic_version"),
                resultSet.getInt("estimated_minutes"));
    }

    private ResourceAsset mapAsset(ResultSet resultSet) throws SQLException {
        AssetMediaType mediaType = AssetMediaType.valueOf(resultSet.getString("media_type"));
        return new ResourceAsset(
                resultSet.getString("asset_key"),
                resultSet.getString("asset_version"),
                mediaType,
                AssetPurpose.valueOf(resultSet.getString("purpose")),
                resultSet.getString("object_key"),
                resultSet.getString("content_hash"),
                resultSet.getString("mime_type"),
                resultSet.getLong("byte_length"),
                AccessScope.valueOf(resultSet.getString("access_scope")),
                readMetadata(resultSet.getString("metadata_json"), mediaType),
                AssetStatus.valueOf(resultSet.getString("status")),
                toInstant(resultSet.getTimestamp("created_at_utc")));
    }

    private AssetMetadata readMetadata(String json, AssetMediaType mediaType) {
        try {
            if (mediaType == AssetMediaType.IMAGE) {
                return objectMapper.readValue(json, ImageAssetMetadata.class);
            }
            return objectMapper.readValue(json, AudioAssetMetadata.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored asset metadata is invalid", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("asset metadata cannot be serialized", exception);
        }
    }

    private Optional<Long> queryId(String sql, Object argument) {
        return jdbcTemplate.query(
                sql,
                resultSet -> resultSet.next() ? Optional.of(resultSet.getLong(1)) : Optional.empty(),
                argument);
    }

    private long insertAndReturnId(PreparedStatementCreator creator) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(creator, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("database did not return a generated id");
        }
        return key.longValue();
    }

    private static PublishedResourceCandidate toCandidate(ResourceCatalogEntry entry) {
        ResourceAsset taskHero = entry.assets().stream()
                .filter(asset -> asset.purpose() == AssetPurpose.TASK_HERO)
                .findFirst()
                .orElseThrow();
        LearningResource resource = entry.resource();
        return new PublishedResourceCandidate(
                resource.resourceKey(),
                entry.resourceVersion().semanticVersion(),
                resource.providerCode(),
                resource.collectionKey(),
                resource.type(),
                resource.title(),
                resource.level(),
                resource.topic(),
                resource.scene(),
                resource.communicationGoal(),
                resource.accessScope(),
                resource.estimatedMinutes(),
                entry.resourceVersion().skillUnitVariantKeys(),
                taskHero,
                entry.assets());
    }

    private static ResourceVersionSaveResult result(
            ResourceCatalogEntry entry,
            CatalogWriteOutcome outcome
    ) {
        return new ResourceVersionSaveResult(
                entry.resource().resourceKey(),
                entry.resourceVersion().semanticVersion(),
                entry.resourceVersion().manifestHash(),
                outcome);
    }

    private static boolean sameImmutableAsset(ResourceAsset existing, ResourceAsset candidate) {
        return existing.assetKey().equals(candidate.assetKey())
                && existing.assetVersion().equals(candidate.assetVersion())
                && existing.mediaType() == candidate.mediaType()
                && existing.purpose() == candidate.purpose()
                && existing.objectKey().equals(candidate.objectKey())
                && existing.contentHash().equals(candidate.contentHash())
                && existing.mimeType().equals(candidate.mimeType())
                && existing.byteLength() == candidate.byteLength()
                && existing.accessScope() == candidate.accessScope()
                && existing.metadata().equals(candidate.metadata())
                && existing.status() == candidate.status()
                && existing.createdAt().toEpochMilli() == candidate.createdAt().toEpochMilli();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    private static Instant toNullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record VersionKey(String resourceKey, String semanticVersion) {
    }
}
