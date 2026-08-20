package cn.forever24.tutor.application.experience;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.experience.Episode;
import cn.forever24.tutor.experience.EpisodeMapping;
import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.ExperienceFitInputs;
import cn.forever24.tutor.experience.ExperienceResolutionRequest;
import cn.forever24.tutor.experience.ExperienceResolutionStatus;
import cn.forever24.tutor.experience.ExperienceStatus;
import cn.forever24.tutor.experience.MappingResourceReference;
import cn.forever24.tutor.experience.Scene;
import cn.forever24.tutor.experience.Season;
import cn.forever24.tutor.experience.StoryTransition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperienceCatalogApplicationServiceTest {

    @Test
    void validatesReferencesThenResolvesThePersistedCatalog() {
        RecordingRepository repository = new RecordingRepository();
        ExperienceCatalogApplicationService service = new ExperienceCatalogApplicationService(
                repository,
                new FixedReferenceResolver(true, true));

        service.replaceCatalog(catalog("season1.ep006.gate-change.b1"));
        var result = service.resolve(new ExperienceResolutionRequest(
                "travel.confirm-information.b1",
                CefrLevel.B1,
                Set.of("travel_communication"),
                Set.of("airport"),
                Set.of("clarification"),
                Set.of(),
                null,
                null));

        assertEquals(ExperienceResolutionStatus.MATCHED, result.status());
        assertEquals("s01.ep006.gate-change.b1", result.mapping().orElseThrow().mappingKey());
    }

    @Test
    void rejectsInvalidResourceReferenceBeforePersistence() {
        RecordingRepository repository = new RecordingRepository();
        ExperienceCatalogApplicationService service = new ExperienceCatalogApplicationService(
                repository,
                new FixedReferenceResolver(true, false));

        assertThrows(IllegalArgumentException.class, () -> service.replaceCatalog(
                catalog("missing.resource.version")));
        assertEquals(Optional.empty(), repository.findCatalog());
    }

    @Test
    void emptyCatalogReturnsExplicitNoMapping() {
        ExperienceCatalogApplicationService service = new ExperienceCatalogApplicationService(
                new RecordingRepository(),
                new FixedReferenceResolver(true, true));

        var result = service.resolve(new ExperienceResolutionRequest(
                "travel.confirm-information.b1", CefrLevel.B1,
                Set.of(), Set.of(), Set.of(), Set.of(), null, null));

        assertEquals(ExperienceResolutionStatus.NO_MAPPING, result.status());
        assertEquals("EXPERIENCE_CATALOG_EMPTY", result.reasonCode());
    }

    private static ExperienceCatalog catalog(String resourceKey) {
        Season season = new Season("S01", "Getting Closer to English", ExperienceStatus.ACTIVE, "{}");
        Episode episode = new Episode(
                "EP006", "S01", "Airport Adventure",
                "Lin Muen needs help confirming a changed boarding gate.",
                false, ExperienceStatus.ACTIVE, "{}", 6);
        Scene scene = new Scene(
                "GATE_CHANGE", "EP006", "Gate Change", "Airport boarding gate",
                "Lin Muen stands near the boarding gate and asks for help.",
                "{\"character\":\"Lin Muen\"}", ExperienceStatus.ACTIVE);
        EpisodeMapping mapping = new EpisodeMapping(
                "s01.ep006.gate-change.b1",
                "travel.confirm-information.b1",
                "S01", "EP006", "GATE_CHANGE",
                Set.of(CefrLevel.B1),
                new StoryTransition(
                        "Lin Muen notices the changed gate and asks the learner to help.",
                        "Lin Muen reaches the correct gate after the learner confirms it.", false),
                new ExperienceFitInputs(
                        Set.of("travel_communication"), Set.of("airport"),
                        Set.of("clarification"), Set.of()),
                null,
                ExperienceStatus.ACTIVE,
                List.of(new MappingResourceReference(resourceKey, "1.0.0", 0)));
        return new ExperienceCatalog(List.of(season), List.of(episode), List.of(scene), List.of(mapping));
    }

    private static final class RecordingRepository implements ExperienceRepository {
        private ExperienceCatalog catalog;

        @Override
        public void replace(ExperienceCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public Optional<ExperienceCatalog> findCatalog() {
            return Optional.ofNullable(catalog);
        }
    }

    private record FixedReferenceResolver(
            boolean variantExists,
            boolean resourceSupportsVariant
    ) implements ExperienceReferenceResolver {

        @Override
        public boolean skillUnitVariantExists(String skillUnitVariantKey) {
            return variantExists;
        }

        @Override
        public boolean resourceVersionSupportsVariant(
                String resourceKey,
                String resourceVersion,
                String skillUnitVariantKey
        ) {
            return resourceSupportsVariant;
        }
    }
}
