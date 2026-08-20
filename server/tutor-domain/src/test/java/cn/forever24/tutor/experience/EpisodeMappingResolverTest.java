package cn.forever24.tutor.experience;

import cn.forever24.tutor.curriculum.CefrLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpisodeMappingResolverTest {

    private final EpisodeMappingResolver resolver = new EpisodeMappingResolver();

    @Test
    void sameSkillCanChooseTheBestOfMultipleEpisodesDeterministically() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "communication.confirm_information.b1",
                Set.of("work_communication"),
                Set.of("meeting"),
                null,
                null));

        assertEquals(ExperienceResolutionStatus.MATCHED, resolution.status());
        assertEquals("s01.ep009.confirm-information.b1", resolution.mapping().orElseThrow().mappingKey());
        assertEquals("EP009", resolution.mapping().orElseThrow().episodeKey());
    }

    @Test
    void equalFitUsesStableMappingKeyTieBreak() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "communication.confirm_information.b1",
                Set.of(),
                Set.of(),
                null,
                null));

        assertEquals("s01.ep002.confirm-information.b1", resolution.mapping().orElseThrow().mappingKey());
    }

    @Test
    void sameEpisodeCanCarryMultipleSkillsWithoutChangingTheSelectedVariant() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "work.report_progress.b1",
                Set.of("work_communication"),
                Set.of("meeting"),
                null,
                null));

        assertEquals("work.report_progress.b1", resolution.mapping().orElseThrow().skillUnitVariantKey());
        assertEquals("EP009", resolution.mapping().orElseThrow().episodeKey());
    }

    @Test
    void preferredIneligibleMappingUsesSameVariantFallback() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "travel.confirm_information.b1",
                Set.of("travel_communication"),
                Set.of("airport"),
                null,
                "s01.ep006.travel-confirm.b2"));

        assertEquals(ExperienceResolutionStatus.FALLBACK_MATCHED, resolution.status());
        assertEquals("s01.ep002.travel-confirm.b1", resolution.mapping().orElseThrow().mappingKey());
        assertEquals(CefrLevel.B1, resolution.mapping().orElseThrow().eligibleLevels().iterator().next());
    }

    @Test
    void storyContinuityCannotOverrideTeachingEligibility() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "travel.confirm_information.b1",
                Set.of("travel_communication"),
                Set.of("airport"),
                "EP006",
                null));

        assertEquals("s01.ep002.travel-confirm.b1", resolution.mapping().orElseThrow().mappingKey());
        assertEquals(0, resolution.storyContinuityScore());
    }

    @Test
    void returnsExplicitNoMappingForUnknownVariant() {
        ExperienceResolution resolution = resolver.resolve(catalog(), request(
                "unknown.variant.b1", Set.of(), Set.of(), null, null));

        assertEquals(ExperienceResolutionStatus.NO_MAPPING, resolution.status());
        assertEquals("NO_MAPPING_FOR_VARIANT", resolution.reasonCode());
    }

    @Test
    void p0RejectsStoryOrderedEpisodes() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(
                "EP001", "S01", "Meet Lin Muen", "The learner meets Lin Muen.",
                true, ExperienceStatus.ACTIVE, "{}", 1));
    }

    @Test
    void fallbackCannotChangeTheSelectedSkillVariant() {
        EpisodeMapping source = mapping(
                "s01.ep002.source.b1", "communication.confirm_information.b1",
                "EP002", "CAFE_ORDER", Set.of(CefrLevel.B1),
                "s01.ep009.other-skill.b1", Set.of("daily_communication"), Set.of("cafe"));
        EpisodeMapping other = mapping(
                "s01.ep009.other-skill.b1", "work.report_progress.b1",
                "EP009", "WORK_MEETING", Set.of(CefrLevel.B1),
                null, Set.of("work_communication"), Set.of("meeting"));

        assertThrows(IllegalArgumentException.class, () -> new ExperienceCatalog(
                seasons(), episodes(), scenes(), List.of(source, other)));
    }

    private static ExperienceResolutionRequest request(
            String variant,
            Set<String> goals,
            Set<String> topics,
            String continuityEpisode,
            String preferredMapping
    ) {
        return new ExperienceResolutionRequest(
                variant,
                CefrLevel.B1,
                goals,
                topics,
                Set.of("clarification"),
                Set.of(),
                continuityEpisode,
                preferredMapping);
    }

    private static ExperienceCatalog catalog() {
        return new ExperienceCatalog(
                seasons(),
                episodes(),
                scenes(),
                List.of(
                        mapping(
                                "s01.ep002.confirm-information.b1",
                                "communication.confirm_information.b1",
                                "EP002", "CAFE_ORDER", Set.of(CefrLevel.B1), null,
                                Set.of("daily_communication"), Set.of("cafe")),
                        mapping(
                                "s01.ep009.confirm-information.b1",
                                "communication.confirm_information.b1",
                                "EP009", "WORK_MEETING", Set.of(CefrLevel.B1), null,
                                Set.of("work_communication"), Set.of("meeting")),
                        mapping(
                                "s01.ep009.report-progress.b1",
                                "work.report_progress.b1",
                                "EP009", "WORK_MEETING", Set.of(CefrLevel.B1), null,
                                Set.of("work_communication"), Set.of("meeting")),
                        mapping(
                                "s01.ep006.travel-confirm.b2",
                                "travel.confirm_information.b1",
                                "EP006", "GATE_CHANGE", Set.of(CefrLevel.B2),
                                "s01.ep002.travel-confirm.b1",
                                Set.of("travel_communication"), Set.of("airport")),
                        mapping(
                                "s01.ep002.travel-confirm.b1",
                                "travel.confirm_information.b1",
                                "EP002", "CAFE_ORDER", Set.of(CefrLevel.B1), null,
                                Set.of("travel_communication"), Set.of("airport"))));
    }

    private static List<Season> seasons() {
        return List.of(new Season(
                "S01", "Getting Closer to English", ExperienceStatus.ACTIVE, "{}"));
    }

    private static List<Episode> episodes() {
        return List.of(
                episode("EP002", "Coffee Shop Morning", 2),
                episode("EP006", "Airport Adventure", 6),
                episode("EP009", "Work Meeting", 9));
    }

    private static Episode episode(String key, String title, int order) {
        return new Episode(
                key, "S01", title, "Lin Muen needs the learner's help in " + title + ".",
                false, ExperienceStatus.ACTIVE, "{}", order);
    }

    private static List<Scene> scenes() {
        return List.of(
                scene("CAFE_ORDER", "EP002", "Coffee shop counter"),
                scene("GATE_CHANGE", "EP006", "Airport boarding gate"),
                scene("WORK_MEETING", "EP009", "Team meeting room"));
    }

    private static Scene scene(String key, String episode, String location) {
        return new Scene(
                key, episode, key, location,
                "Lin Muen participates in a concrete English communication task.",
                "{\"character\":\"Lin Muen\"}", ExperienceStatus.ACTIVE);
    }

    private static EpisodeMapping mapping(
            String key,
            String variant,
            String episode,
            String scene,
            Set<CefrLevel> levels,
            String fallback,
            Set<String> goals,
            Set<String> topics
    ) {
        return new EpisodeMapping(
                key,
                variant,
                "S01",
                episode,
                scene,
                levels,
                new StoryTransition(
                        "Lin Muen invites the learner into the current communication task.",
                        "Lin Muen can continue after the learner completes the task.",
                        false),
                new ExperienceFitInputs(goals, topics, Set.of("clarification"), Set.of()),
                fallback,
                ExperienceStatus.ACTIVE,
                List.of(new MappingResourceReference("resource." + key, "1.0.0", 0)));
    }
}
