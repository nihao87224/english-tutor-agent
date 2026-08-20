import { describe, expect, it } from "vitest";
import { buildScenarioLesson, lessonLoaded, ScenarioLessonContentError, taskHeroStyle, withMediaFailure } from "./scenarioLessonModel";
import { fixtureLearningResource, fixtureLessonSession } from "./scenarioLessonTestFixtures";

describe("scenarioLessonModel", () => {
  it("requires a scene-specific Lin Muen task hero", () => {
    const resource = fixtureLearningResource();
    resource.taskHero = null;

    expect(() => buildScenarioLesson(resource)).toThrowError(new ScenarioLessonContentError("A task_hero image is required"));
  });

  it("rejects an avatar-only UI fixture", () => {
    const resource = fixtureLearningResource();
    resource.taskHero!.metadata = {
      ...resource.taskHero!.metadata,
      shotType: "portrait_avatar",
      displaySurfaces: ["avatar"],
    };

    expect(() => buildScenarioLesson(resource)).toThrow(/Avatar-only/);
  });

  it("rejects a different lead character", () => {
    const resource = fixtureLearningResource();
    (resource.content.lessonPackage as Record<string, unknown>).character = "Someone Else";

    expect(() => buildScenarioLesson(resource)).toThrow(/must be Lin Muen/);
  });

  it("uses the locked session step and exact resource version when resuming", () => {
    const session = fixtureLessonSession("TRANSCRIPT_EXPRESSIONS");
    const loaded = lessonLoaded(session, fixtureLearningResource(), {
      imageUrl: "https://cdn.test/hero.webp",
      audioUrl: "https://cdn.test/dialogue.mp3",
    });

    expect(loaded.session.currentStep).toBe("TRANSCRIPT_EXPRESSIONS");
    expect(loaded.lesson.resourceVersion).toBe("1.0.0");
  });

  it("rejects a resource that differs from the session lock", () => {
    const resource = fixtureLearningResource();
    resource.resourceVersion = "2.0.0";
    expect(() => lessonLoaded(fixtureLessonSession(), resource, {})).toThrow(/session-locked version/);
  });

  it("provides independent image and audio fallback state", () => {
    const loaded = lessonLoaded(fixtureLessonSession(), fixtureLearningResource(), {
      imageUrl: "https://cdn.test/hero.webp",
      audioUrl: "https://cdn.test/dialogue.mp3",
    });

    expect(withMediaFailure(loaded, "image")).toMatchObject({ imageUnavailable: true, audioUnavailable: false });
    expect(withMediaFailure(loaded, "audio")).toMatchObject({ imageUnavailable: false, audioUnavailable: true });
  });

  it("maps focalPoint to a responsive object-position", () => {
    const lesson = buildScenarioLesson(fixtureLearningResource());
    expect(taskHeroStyle(lesson.taskHero)).toEqual({ aspectRatio: "16 / 9", objectPosition: "62% 48%" });
  });
});
