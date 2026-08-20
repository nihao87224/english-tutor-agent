package cn.forever24.tutor.infrastructure.experience;

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

import java.util.List;
import java.util.Set;

final class SeasonOneExperienceFixture {

    private SeasonOneExperienceFixture() {
    }

    static ExperienceCatalog catalog() {
        Season season = new Season(
                "S01", "Getting Closer to English", ExperienceStatus.ACTIVE,
                "{\"character\":\"Lin Muen\"}");
        List<Episode> episodes = List.of(
                episode("EP002", "Coffee Shop Morning", 2),
                episode("EP006", "Airport Adventure", 6),
                episode("EP009", "Work Meeting", 9));
        List<Scene> scenes = List.of(
                scene("CAFE_ORDER", "EP002", "Coffee shop counter"),
                scene("GATE_CHANGE", "EP006", "Airport boarding gate"),
                scene("WORK_MEETING", "EP009", "Team meeting room"));
        List<EpisodeMapping> mappings = List.of(
                mapping(
                        "s01.ep002.order-drink.a2", "service.order-drink.a2",
                        "EP002", "CAFE_ORDER", CefrLevel.A2, null,
                        "season1.ep002.order-drink.a2", "daily_communication", "cafe"),
                mapping(
                        "s01.ep002.confirm-information.b1", "travel.confirm-information.b1",
                        "EP002", "CAFE_ORDER", CefrLevel.B1, null,
                        "season1.ep002.confirm-information.b1", "daily_communication", "cafe"),
                mapping(
                        "s01.ep006.confirm-information.b1", "travel.confirm-information.b1",
                        "EP006", "GATE_CHANGE", CefrLevel.B1,
                        "s01.ep002.confirm-information.b1",
                        "season1.ep006.gate-change.b1", "travel_communication", "airport"),
                mapping(
                        "s01.ep009.report-progress.b1", "work.report-progress.b1",
                        "EP009", "WORK_MEETING", CefrLevel.B1, null,
                        "season1.ep009.report-progress.b1", "work_communication", "meeting"),
                mapping(
                        "s01.ep009.ask-clarification.b1", "work.ask-clarification.b1",
                        "EP009", "WORK_MEETING", CefrLevel.B1, null,
                        "season1.ep009.ask-clarification.b1", "work_communication", "meeting"));
        return new ExperienceCatalog(List.of(season), episodes, scenes, mappings);
    }

    private static Episode episode(String key, String title, int order) {
        return new Episode(
                key, "S01", title,
                "Lin Muen asks the learner to help with a real communication task in " + title + ".",
                false, ExperienceStatus.ACTIVE, "{}", order);
    }

    private static Scene scene(String key, String episode, String location) {
        return new Scene(
                key, episode, key, location,
                "Lin Muen is visibly present and participates in the current scene.",
                "{\"character\":\"Lin Muen\"}", ExperienceStatus.ACTIVE);
    }

    private static EpisodeMapping mapping(
            String key,
            String variant,
            String episode,
            String scene,
            CefrLevel level,
            String fallback,
            String resource,
            String goal,
            String topic
    ) {
        return new EpisodeMapping(
                key, variant, "S01", episode, scene, Set.of(level),
                new StoryTransition(
                        "Lin Muen naturally introduces the learner to this communication task.",
                        "Lin Muen completes the current story beat after valid learner evidence.",
                        false),
                new ExperienceFitInputs(
                        Set.of(goal), Set.of(topic), Set.of("clarification"), Set.of()),
                fallback,
                ExperienceStatus.ACTIVE,
                List.of(new MappingResourceReference(resource, "1.0.0", 0)));
    }
}
