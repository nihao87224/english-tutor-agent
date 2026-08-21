import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { I18nProvider } from "../../shared/i18n";
import { lessonLoaded, withMediaFailure } from "./scenarioLessonModel";
import { ScenarioLessonContent } from "./ScenarioLessonPage";
import { fixtureLearningResource, fixtureLessonSession } from "./scenarioLessonTestFixtures";

describe("ScenarioLessonContent", () => {
  it("renders the airport task hero as the scene, not as an avatar", () => {
    const html = render("SCENE_CONTEXT");

    expect(html).toContain("Lin Muen stands full body near an airport boarding gate");
    expect(html).toContain("object-position:62% 48%");
    expect(html).toContain("Help Lin Muen confirm the new gate");
    expect(html).toContain('aria-label="场景信息"');
  });

  it("keeps transcript content hidden on first render with an accessible disclosure", () => {
    const html = render("FIRST_LISTEN");

    expect(html).toContain('aria-expanded="false"');
    expect(html).toContain('aria-controls="scenario-transcript"');
    expect(html).not.toContain('id="scenario-transcript"');
    expect(html).toContain("<audio");
    expect(html).toContain('controls=""');
  });

  it("shows image and audio fallback without blocking transcript access", () => {
    const session = fixtureLessonSession("FIRST_LISTEN");
    const loaded = lessonLoaded(session, fixtureLearningResource(), {
      imageUrl: "https://cdn.test/hero.webp",
      audioUrl: "https://cdn.test/dialogue.mp3",
    });
    const fallback = withMediaFailure(withMediaFailure(loaded, "image"), "audio");
    const html = renderValue(fallback);

    expect(html).toContain("场景图暂不可用，课程内容仍可继续");
    expect(html).toContain("音频暂不可用");
    expect(html).toContain("展开 Transcript 与 Expressions");
  });

  it("keeps the full scene image in the speaking area", () => {
    const html = render("GUIDED_SPEAKING");

    expect(html).toContain("cdn.test/hero.webp");
    expect(html).toContain("把关键信息说给 Lin Muen");
    expect(html).toContain("Your flight leaves from...");
    expect(html).toContain("提交口语文本");
  });

  it("renders half-duplex recording with a text fallback", () => {
    const value = lessonLoaded(fixtureLessonSession("GUIDED_SPEAKING"), fixtureLearningResource(), {
      imageUrl: "https://cdn.test/hero.webp",
      audioUrl: "https://cdn.test/dialogue.mp3",
    });
    const html = renderValue(value, true);

    expect(html).toContain("按住节奏，说给 Lin Muen 听");
    expect(html).toContain("开始录音");
    expect(html).toContain("提交口语文本");
    expect(html).toContain("录音仅用于本次学习");
  });

  it("keeps the Lin Muen scene visible around the bounded role-play dialogue", () => {
    const html = render("ROLE_PLAY");

    expect(html).toContain("cdn.test/hero.webp");
    expect(html).toContain("和 Lin Muen 一起完成真实对话");
    expect(html).toContain("Traveler helping Lin Muen");
    expect(html).toContain("Airport agent");
    expect(html).toContain("How can I help you and Lin Muen?");
  });

  it("renders the first server-owned remaining comprehension question", () => {
    const html = render("COMPREHENSION");

    expect(html).toContain("Which gate should Lin Muen use?");
    expect(html).toContain("提交理解答案");
    expect(html).toContain("disabled");
  });

  it("renders the server-paused state with an explicit resume action", () => {
    const session = fixtureLessonSession("FIRST_LISTEN");
    session.status = "PAUSED";
    const html = renderValue(lessonLoaded(session, fixtureLearningResource(), {
      imageUrl: "https://cdn.test/hero.webp",
      audioUrl: "https://cdn.test/dialogue.mp3",
    }));

    expect(html).toContain(">继续课程</button>");
    expect(html).toContain("课程已暂停。继续后才会记录下一步。");
    expect(html).toContain("disabled");
  });
});

function render(step: Parameters<typeof fixtureLessonSession>[0]): string {
  return renderValue(lessonLoaded(fixtureLessonSession(step), fixtureLearningResource(), {
    imageUrl: "https://cdn.test/hero.webp",
    audioUrl: "https://cdn.test/dialogue.mp3",
  }));
}

function renderValue(value: ReturnType<typeof lessonLoaded>, withVoice = false): string {
  return renderToStaticMarkup(createElement(I18nProvider, {
    initialLocale: "zh-CN",
    children: createElement(ScenarioLessonContent, {
      value,
      onBack: vi.fn(),
      onImageError: vi.fn(),
      onAudioError: vi.fn(),
      onPause: vi.fn(),
      onResume: vi.fn(),
      onCompleteStep: vi.fn(),
      onSubmitAttempt: vi.fn(),
      onSubmitAudioAttempt: withVoice ? vi.fn() : undefined,
      onConfirmTranscript: withVoice ? vi.fn() : undefined,
      onListRolePlayTurns: vi.fn().mockResolvedValue([]),
      onStreamRolePlayMessage: vi.fn().mockResolvedValue(undefined),
    }),
  }));
}
