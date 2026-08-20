package cn.forever24.tutor.experience;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EpisodeMappingResolver {

    public ExperienceResolution resolve(ExperienceCatalog catalog, ExperienceResolutionRequest request) {
        if (catalog == null || request == null) {
            throw new IllegalArgumentException("catalog and request are required");
        }
        List<EpisodeMapping> forVariant = catalog.mappings().stream()
                .filter(mapping -> mapping.skillUnitVariantKey().equals(request.skillUnitVariantKey()))
                .toList();
        if (forVariant.isEmpty()) {
            return ExperienceResolution.noMapping("NO_MAPPING_FOR_VARIANT");
        }

        Map<String, EpisodeMapping> mappingsByKey = catalog.mappingsByKey();
        if (request.preferredMappingKey() != null) {
            EpisodeMapping preferred = mappingsByKey.get(request.preferredMappingKey());
            if (preferred != null && preferred.skillUnitVariantKey().equals(request.skillUnitVariantKey())) {
                if (isEligible(preferred, request)) {
                    return matched(preferred, request, ExperienceResolutionStatus.MATCHED, "PREFERRED_MAPPING");
                }
                Optional<EpisodeMapping> fallback = firstEligibleFallback(preferred, mappingsByKey, request);
                if (fallback.isPresent()) {
                    return matched(
                            fallback.get(),
                            request,
                            ExperienceResolutionStatus.FALLBACK_MATCHED,
                            "PREFERRED_MAPPING_FALLBACK");
                }
            }
        }

        return forVariant.stream()
                .filter(mapping -> isEligible(mapping, request))
                .map(mapping -> new ScoredMapping(
                        mapping,
                        learnerFitScore(mapping, request),
                        storyScore(mapping, request)))
                .sorted(Comparator.comparingInt(ScoredMapping::learnerFitScore).reversed()
                        .thenComparing(Comparator.comparingInt(ScoredMapping::storyScore).reversed())
                        .thenComparing(scored -> scored.mapping().mappingKey()))
                .findFirst()
                .map(scored -> new ExperienceResolution(
                        ExperienceResolutionStatus.MATCHED,
                        Optional.of(scored.mapping()),
                        scored.learnerFitScore(),
                        scored.storyScore(),
                        "BEST_ELIGIBLE_MAPPING"))
                .orElseGet(() -> ExperienceResolution.noMapping("NO_ELIGIBLE_MAPPING"));
    }

    private static Optional<EpisodeMapping> firstEligibleFallback(
            EpisodeMapping start,
            Map<String, EpisodeMapping> mappingsByKey,
            ExperienceResolutionRequest request
    ) {
        EpisodeMapping current = start.fallbackMappingKey() == null
                ? null
                : mappingsByKey.get(start.fallbackMappingKey());
        while (current != null) {
            if (!current.skillUnitVariantKey().equals(request.skillUnitVariantKey())) {
                return Optional.empty();
            }
            if (isEligible(current, request)) {
                return Optional.of(current);
            }
            current = current.fallbackMappingKey() == null
                    ? null
                    : mappingsByKey.get(current.fallbackMappingKey());
        }
        return Optional.empty();
    }

    private static ExperienceResolution matched(
            EpisodeMapping mapping,
            ExperienceResolutionRequest request,
            ExperienceResolutionStatus status,
            String reason
    ) {
        return new ExperienceResolution(
                status,
                Optional.of(mapping),
                learnerFitScore(mapping, request),
                storyScore(mapping, request),
                reason);
    }

    private static boolean isEligible(EpisodeMapping mapping, ExperienceResolutionRequest request) {
        return mapping.status() == ExperienceStatus.ACTIVE
                && mapping.eligibleLevels().contains(request.learnerLevel())
                && java.util.Collections.disjoint(
                        mapping.fitInputs().contraindications(),
                        request.contraindications());
    }

    private static int learnerFitScore(EpisodeMapping mapping, ExperienceResolutionRequest request) {
        ExperienceFitInputs fit = mapping.fitInputs();
        return overlap(fit.goalTags(), request.goalTags()) * 100
                + overlap(fit.topicTags(), request.topicTags()) * 10
                + overlap(fit.interactionTags(), request.interactionTags()) * 5;
    }

    private static int storyScore(EpisodeMapping mapping, ExperienceResolutionRequest request) {
        return request.continuityEpisodeKey() != null
                && request.continuityEpisodeKey().equals(mapping.episodeKey()) ? 1 : 0;
    }

    private static int overlap(Set<String> mappingTags, Set<String> learnerTags) {
        return (int) mappingTags.stream().filter(learnerTags::contains).count();
    }

    private record ScoredMapping(EpisodeMapping mapping, int learnerFitScore, int storyScore) {
    }
}
