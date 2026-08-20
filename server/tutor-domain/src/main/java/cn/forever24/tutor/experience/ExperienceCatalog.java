package cn.forever24.tutor.experience;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ExperienceCatalog(
        List<Season> seasons,
        List<Episode> episodes,
        List<Scene> scenes,
        List<EpisodeMapping> mappings
) {
    public ExperienceCatalog {
        seasons = requiredCopy(seasons, "seasons");
        episodes = requiredCopy(episodes, "episodes");
        scenes = requiredCopy(scenes, "scenes");
        mappings = requiredCopy(mappings, "mappings");
        validate(seasons, episodes, scenes, mappings);
    }

    private static void validate(
            List<Season> seasons,
            List<Episode> episodes,
            List<Scene> scenes,
            List<EpisodeMapping> mappings
    ) {
        Map<String, Season> seasonByKey = unique(seasons, Season::seasonKey, "season");
        Map<String, Episode> episodeByKey = unique(episodes, Episode::episodeKey, "episode");
        Map<String, Scene> sceneByKey = unique(scenes, Scene::sceneKey, "scene");
        Map<String, EpisodeMapping> mappingByKey = unique(mappings, EpisodeMapping::mappingKey, "mapping");

        for (Episode episode : episodes) {
            if (!seasonByKey.containsKey(episode.seasonKey())) {
                throw new IllegalArgumentException("episode references unknown season: " + episode.seasonKey());
            }
        }
        for (Scene scene : scenes) {
            if (!episodeByKey.containsKey(scene.episodeKey())) {
                throw new IllegalArgumentException("scene references unknown episode: " + scene.episodeKey());
            }
        }
        for (EpisodeMapping mapping : mappings) {
            Episode episode = episodeByKey.get(mapping.episodeKey());
            Scene scene = sceneByKey.get(mapping.sceneKey());
            if (episode == null || scene == null) {
                throw new IllegalArgumentException("mapping references an unknown episode or scene");
            }
            if (!episode.seasonKey().equals(mapping.seasonKey())
                    || !scene.episodeKey().equals(mapping.episodeKey())) {
                throw new IllegalArgumentException("mapping season/episode/scene references are inconsistent");
            }
            if (mapping.status() == ExperienceStatus.ACTIVE
                    && (seasonByKey.get(mapping.seasonKey()).status() != ExperienceStatus.ACTIVE
                    || episode.status() != ExperienceStatus.ACTIVE
                    || scene.status() != ExperienceStatus.ACTIVE)) {
                throw new IllegalArgumentException("active mapping requires active season, episode and scene");
            }
            if (mapping.fallbackMappingKey() != null) {
                EpisodeMapping fallback = mappingByKey.get(mapping.fallbackMappingKey());
                if (fallback == null) {
                    throw new IllegalArgumentException("mapping references unknown fallback: "
                            + mapping.fallbackMappingKey());
                }
                if (!mapping.skillUnitVariantKey().equals(fallback.skillUnitVariantKey())) {
                    throw new IllegalArgumentException("fallback must preserve the selected Skill Unit Variant");
                }
            }
        }
        assertNoFallbackCycles(mappingByKey);
    }

    private static void assertNoFallbackCycles(Map<String, EpisodeMapping> mappings) {
        for (EpisodeMapping mapping : mappings.values()) {
            Set<String> path = new HashSet<>();
            EpisodeMapping current = mapping;
            while (current != null) {
                if (!path.add(current.mappingKey())) {
                    throw new IllegalArgumentException("episode mapping fallback contains a cycle at: "
                            + current.mappingKey());
                }
                current = current.fallbackMappingKey() == null
                        ? null
                        : mappings.get(current.fallbackMappingKey());
            }
        }
    }

    private static <T> List<T> requiredCopy(List<T> values, String field) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return List.copyOf(values);
    }

    private static <T> Map<String, T> unique(
            List<T> values,
            Function<T, String> keyExtractor,
            String type
    ) {
        Map<String, T> result = new HashMap<>();
        for (T value : values) {
            String key = keyExtractor.apply(value);
            if (result.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("duplicate " + type + " key: " + key);
            }
        }
        return Map.copyOf(result);
    }

    public Map<String, EpisodeMapping> mappingsByKey() {
        return mappings.stream().collect(Collectors.toUnmodifiableMap(
                EpisodeMapping::mappingKey,
                Function.identity()));
    }
}
