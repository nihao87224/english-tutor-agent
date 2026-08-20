package cn.forever24.tutor.infrastructure.experience;

import cn.forever24.tutor.application.experience.ExperienceRepository;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.experience.Episode;
import cn.forever24.tutor.experience.EpisodeMapping;
import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.ExperienceFitInputs;
import cn.forever24.tutor.experience.ExperienceStatus;
import cn.forever24.tutor.experience.MappingResourceReference;
import cn.forever24.tutor.experience.Scene;
import cn.forever24.tutor.experience.Season;
import cn.forever24.tutor.experience.StoryTransition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JdbcExperienceRepository implements ExperienceRepository {

    private static final TypeReference<List<CefrLevel>> CEFR_LEVELS = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcExperienceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void replace(ExperienceCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        jdbcTemplate.update("DELETE FROM experience_season");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Map<String, Long> seasonIds = persistSeasons(catalog.seasons(), now);
        Map<String, Long> episodeIds = persistEpisodes(catalog.episodes(), seasonIds, now);
        Map<String, Long> sceneIds = persistScenes(catalog.scenes(), episodeIds, now);
        Map<String, Long> mappingIds = persistMappings(catalog.mappings(), episodeIds, sceneIds, now);
        persistFallbacks(catalog.mappings(), mappingIds);
        persistResourceReferences(catalog.mappings(), mappingIds);
    }

    @Override
    public Optional<ExperienceCatalog> findCatalog() {
        List<Season> seasons = jdbcTemplate.query("""
                        SELECT season_key, title, status, metadata_json
                        FROM experience_season ORDER BY season_key
                        """,
                (resultSet, rowNumber) -> new Season(
                        resultSet.getString("season_key"),
                        resultSet.getString("title"),
                        ExperienceStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("metadata_json")));
        if (seasons.isEmpty()) {
            return Optional.empty();
        }
        List<Episode> episodes = jdbcTemplate.query("""
                        SELECT e.episode_key, s.season_key, e.title, e.story_anchor,
                               e.story_order_required, e.status, e.metadata_json, e.sequence_no
                        FROM experience_episode e
                        JOIN experience_season s ON s.id = e.season_id
                        ORDER BY e.episode_key
                        """,
                (resultSet, rowNumber) -> new Episode(
                        resultSet.getString("episode_key"),
                        resultSet.getString("season_key"),
                        resultSet.getString("title"),
                        resultSet.getString("story_anchor"),
                        resultSet.getBoolean("story_order_required"),
                        ExperienceStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("metadata_json"),
                        resultSet.getInt("sequence_no")));
        List<Scene> scenes = jdbcTemplate.query("""
                        SELECT scene.scene_key, episode.episode_key, scene.title, scene.location,
                               scene.story_context, scene.character_state_json, scene.status
                        FROM experience_scene scene
                        JOIN experience_episode episode ON episode.id = scene.episode_id
                        ORDER BY scene.scene_key
                        """,
                (resultSet, rowNumber) -> new Scene(
                        resultSet.getString("scene_key"),
                        resultSet.getString("episode_key"),
                        resultSet.getString("title"),
                        resultSet.getString("location"),
                        resultSet.getString("story_context"),
                        resultSet.getString("character_state_json"),
                        ExperienceStatus.valueOf(resultSet.getString("status"))));
        List<EpisodeMapping> mappings = loadMappings();
        return Optional.of(new ExperienceCatalog(seasons, episodes, scenes, mappings));
    }

    private Map<String, Long> persistSeasons(List<Season> seasons, LocalDateTime now) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Season season : seasons) {
            long id = insertAndReturnId(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO experience_season (
                            season_key, title, status, metadata_json,
                            created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, ?, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, season.seasonKey());
                statement.setString(2, season.title());
                statement.setString(3, season.status().name());
                statement.setString(4, season.metadataJson());
                statement.setTimestamp(5, Timestamp.valueOf(now));
                statement.setTimestamp(6, Timestamp.valueOf(now));
                return statement;
            });
            ids.put(season.seasonKey(), id);
        }
        return Map.copyOf(ids);
    }

    private Map<String, Long> persistEpisodes(
            List<Episode> episodes,
            Map<String, Long> seasonIds,
            LocalDateTime now
    ) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Episode episode : episodes) {
            long id = insertAndReturnId(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO experience_episode (
                            episode_key, season_id, title, story_anchor, story_order_required,
                            status, metadata_json, sequence_no, created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, episode.episodeKey());
                statement.setLong(2, requiredId(seasonIds, episode.seasonKey(), "season"));
                statement.setString(3, episode.title());
                statement.setString(4, episode.storyAnchor());
                statement.setBoolean(5, episode.storyOrderRequired());
                statement.setString(6, episode.status().name());
                statement.setString(7, episode.metadataJson());
                statement.setInt(8, episode.sequenceNumber());
                statement.setTimestamp(9, Timestamp.valueOf(now));
                statement.setTimestamp(10, Timestamp.valueOf(now));
                return statement;
            });
            ids.put(episode.episodeKey(), id);
        }
        return Map.copyOf(ids);
    }

    private Map<String, Long> persistScenes(
            List<Scene> scenes,
            Map<String, Long> episodeIds,
            LocalDateTime now
    ) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Scene scene : scenes) {
            long id = insertAndReturnId(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO experience_scene (
                            scene_key, episode_id, title, location, story_context,
                            character_state_json, status, created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, scene.sceneKey());
                statement.setLong(2, requiredId(episodeIds, scene.episodeKey(), "episode"));
                statement.setString(3, scene.title());
                statement.setString(4, scene.location());
                statement.setString(5, scene.storyContext());
                statement.setString(6, scene.characterStateJson());
                statement.setString(7, scene.status().name());
                statement.setTimestamp(8, Timestamp.valueOf(now));
                statement.setTimestamp(9, Timestamp.valueOf(now));
                return statement;
            });
            ids.put(scene.sceneKey(), id);
        }
        return Map.copyOf(ids);
    }

    private Map<String, Long> persistMappings(
            List<EpisodeMapping> mappings,
            Map<String, Long> episodeIds,
            Map<String, Long> sceneIds,
            LocalDateTime now
    ) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (EpisodeMapping mapping : mappings) {
            long variantId = queryId(
                    "SELECT id FROM curriculum_skill_unit_variant WHERE variant_key = ?",
                    mapping.skillUnitVariantKey()).orElseThrow(() -> new IllegalArgumentException(
                    "unknown Skill Unit Variant reference: " + mapping.skillUnitVariantKey()));
            long id = insertAndReturnId(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO episode_mapping (
                            mapping_key, variant_id, episode_id, scene_id, eligible_levels_json,
                            learner_fit_json, story_transition_json, fit_inputs_json,
                            fallback_mapping_id, status, created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, mapping.mappingKey());
                statement.setLong(2, variantId);
                statement.setLong(3, requiredId(episodeIds, mapping.episodeKey(), "episode"));
                statement.setLong(4, requiredId(sceneIds, mapping.sceneKey(), "scene"));
                statement.setString(5, writeJson(mapping.eligibleLevels()));
                statement.setString(6, writeJson(new StoredLearnerFit(
                        mapping.fitInputs().goalTags(), mapping.fitInputs().contraindications())));
                statement.setString(7, writeJson(mapping.storyTransition()));
                statement.setString(8, writeJson(mapping.fitInputs()));
                statement.setString(9, mapping.status().name());
                statement.setTimestamp(10, Timestamp.valueOf(now));
                statement.setTimestamp(11, Timestamp.valueOf(now));
                return statement;
            });
            ids.put(mapping.mappingKey(), id);
        }
        return Map.copyOf(ids);
    }

    private void persistFallbacks(List<EpisodeMapping> mappings, Map<String, Long> mappingIds) {
        for (EpisodeMapping mapping : mappings) {
            if (mapping.fallbackMappingKey() != null) {
                jdbcTemplate.update(
                        "UPDATE episode_mapping SET fallback_mapping_id = ? WHERE id = ?",
                        requiredId(mappingIds, mapping.fallbackMappingKey(), "fallback mapping"),
                        requiredId(mappingIds, mapping.mappingKey(), "mapping"));
            }
        }
    }

    private void persistResourceReferences(List<EpisodeMapping> mappings, Map<String, Long> mappingIds) {
        for (EpisodeMapping mapping : mappings) {
            for (MappingResourceReference reference : mapping.resources()) {
                long versionId = jdbcTemplate.query("""
                                SELECT rv.id
                                FROM learning_resource_version rv
                                JOIN learning_resource resource ON resource.id = rv.resource_id
                                WHERE resource.resource_key = ? AND rv.semantic_version = ?
                                """,
                        resultSet -> resultSet.next()
                                ? resultSet.getLong("id")
                                : -1L,
                        reference.resourceKey(),
                        reference.resourceVersion());
                if (versionId < 0) {
                    throw new IllegalArgumentException("unknown resource version reference: "
                            + reference.resourceKey() + "@" + reference.resourceVersion());
                }
                jdbcTemplate.update("""
                                INSERT INTO episode_mapping_resource (
                                    mapping_id, resource_version_id, priority
                                ) VALUES (?, ?, ?)
                                """,
                        requiredId(mappingIds, mapping.mappingKey(), "mapping"),
                        versionId,
                        reference.priority());
            }
        }
    }

    private List<EpisodeMapping> loadMappings() {
        return jdbcTemplate.query("""
                        SELECT mapping.id, mapping.mapping_key, variant.variant_key,
                               season.season_key, episode.episode_key, scene.scene_key,
                               mapping.eligible_levels_json, mapping.story_transition_json,
                               mapping.fit_inputs_json, fallback.mapping_key AS fallback_mapping_key,
                               mapping.status
                        FROM episode_mapping mapping
                        JOIN curriculum_skill_unit_variant variant ON variant.id = mapping.variant_id
                        JOIN experience_episode episode ON episode.id = mapping.episode_id
                        JOIN experience_season season ON season.id = episode.season_id
                        JOIN experience_scene scene ON scene.id = mapping.scene_id
                        LEFT JOIN episode_mapping fallback ON fallback.id = mapping.fallback_mapping_id
                        ORDER BY mapping.mapping_key
                        """,
                (resultSet, rowNumber) -> new EpisodeMapping(
                        resultSet.getString("mapping_key"),
                        resultSet.getString("variant_key"),
                        resultSet.getString("season_key"),
                        resultSet.getString("episode_key"),
                        resultSet.getString("scene_key"),
                        Set.copyOf(readJson(resultSet.getString("eligible_levels_json"), CEFR_LEVELS)),
                        readJson(resultSet.getString("story_transition_json"), StoryTransition.class),
                        readJson(resultSet.getString("fit_inputs_json"), ExperienceFitInputs.class),
                        resultSet.getString("fallback_mapping_key"),
                        ExperienceStatus.valueOf(resultSet.getString("status")),
                        loadResourceReferences(resultSet.getLong("id"))));
    }

    private List<MappingResourceReference> loadResourceReferences(long mappingId) {
        return jdbcTemplate.query("""
                        SELECT resource.resource_key, version.semantic_version, reference.priority
                        FROM episode_mapping_resource reference
                        JOIN learning_resource_version version ON version.id = reference.resource_version_id
                        JOIN learning_resource resource ON resource.id = version.resource_id
                        WHERE reference.mapping_id = ?
                        ORDER BY reference.priority
                        """,
                (resultSet, rowNumber) -> new MappingResourceReference(
                        resultSet.getString("resource_key"),
                        resultSet.getString("semantic_version"),
                        resultSet.getInt("priority")),
                mappingId);
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("experience metadata cannot be serialized", exception);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored experience metadata is invalid", exception);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored experience metadata is invalid", exception);
        }
    }

    private static long requiredId(Map<String, Long> ids, String key, String type) {
        Long id = ids.get(key);
        if (id == null) {
            throw new IllegalArgumentException("unknown " + type + " reference: " + key);
        }
        return id;
    }

    private record StoredLearnerFit(Set<String> goalTags, Set<String> contraindications) {
    }
}
