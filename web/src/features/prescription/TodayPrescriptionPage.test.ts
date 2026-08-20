import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import type { DailyLearningPrescription } from "../../shared/api";
import { I18nProvider } from "../../shared/i18n";
import { PrescriptionContent } from "./TodayPrescriptionPage";

describe("PrescriptionContent", () => {
  it("renders a scene-specific Lin Muen task hero with focal-point crop", () => {
    const html = renderPrescription(fixturePrescription());

    expect(html).toContain('alt="Lin Muen stands full body beside airport gate A17."');
    expect(html).toContain("object-position:68% 42%");
    expect(html).toContain("aspect-ratio:16 / 9");
    expect(html).toContain("Airport Adventure");
  });

  it("renders all three feedback categories with native keyboard-operable controls", () => {
    const html = renderPrescription(fixturePrescription());

    expect(html).toContain(">太难了</button>");
    expect(html).toContain(">太简单</button>");
    expect(html).toContain('id="available-minutes"');
    expect(html).toContain(">按时间重排</button>");
    expect(html).toContain(">今天不想练这个主题</button>");
    expect((html.match(/type="button"/g) ?? []).length).toBeGreaterThanOrEqual(6);
  });

  it("visibly changes the teaching target and Lin Muen scene for a different backend prescription", () => {
    const work = fixturePrescription();
    work.priorityGoal = { code: "WORKPLACE", label: "清晰解释项目延期" };
    work.experience = { seasonId: "S01", episodeId: "EP009", sceneId: "work_meeting", title: "Work Meeting" };
    work.blocks[0] = {
      ...work.blocks[0],
      title: "解释原因和下一步",
      taskHero: {
        ...work.blocks[0].taskHero,
        assetId: "hero-meeting",
        url: "https://cdn.example.test/meeting.webp",
        altText: "Lin Muen stands by a meeting screen and explains a delay.",
      },
    };

    const html = renderPrescription(work);
    expect(html).toContain("清晰解释项目延期");
    expect(html).toContain("Work Meeting");
    expect(html).toContain("meeting.webp");
    expect(html).not.toContain("Airport Adventure");
  });
});

function renderPrescription(prescription: DailyLearningPrescription): string {
  return renderToStaticMarkup(
    createElement(I18nProvider, {
      initialLocale: "zh-CN",
      children: createElement(PrescriptionContent, {
        prescription,
        regenerating: false,
        quota: {
          unlimited: false,
          quotaDate: "2026-08-20",
          dailyLimit: 20,
          used: 2,
          bonus: 0,
          remaining: 18,
          resetAt: "2026-08-21T00:00:00Z",
        },
        quotaLoading: false,
        availableMinutes: 10,
        onAvailableMinutesChange: vi.fn(),
        onRegenerate: vi.fn(),
        onRefreshQuota: vi.fn(async () => undefined),
        onOpenAccount: vi.fn(),
      }),
    }),
  );
}

function fixturePrescription(): DailyLearningPrescription {
  return {
    prescriptionId: "prx-airport",
    version: 3,
    learningDate: "2026-08-20",
    timezone: "Asia/Shanghai",
    status: "ACTIVE",
    priorityGoal: { code: "TRAVEL", label: "确认并复述机场信息" },
    rationale: "旅行目标优先，而且确认信息技能今天到期复习。",
    reasonCodes: ["GOAL_MATCH", "REVIEW_DUE"],
    estimatedMinutes: 20,
    experience: { seasonId: "S01", episodeId: "EP006", sceneId: "airport_gate", title: "Airport Adventure" },
    blocks: [
      {
        blockId: "output",
        sequence: 1,
        type: "OUTPUT",
        title: "确认新的登机口",
        skillUnitVariantId: "travel.confirm_gate_change.b1",
        resource: { resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0" },
        episodeMappingId: "map-airport",
        difficulty: "B1",
        scaffolding: "MEDIUM",
        trainingType: "ROLE_PLAY",
        estimatedMinutes: 8,
        expectedEvidence: ["confirm_gate", "confirm_time"],
        recommendationFactors: { goalMatch: 0.9 },
        taskHero: {
          assetId: "hero-airport",
          url: "https://cdn.example.test/airport.webp",
          aspectRatio: "16:9",
          focalPoint: { x: 0.68, y: 0.42 },
          altText: "Lin Muen stands full body beside airport gate A17.",
        },
        status: "READY",
      },
    ],
    generatedAt: "2026-08-20T00:00:00Z",
    expiresAt: "2026-08-20T16:00:00Z",
  };
}
