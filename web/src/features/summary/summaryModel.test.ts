import { describe, expect, it } from "vitest";
import type { TrainingSessionCompletion } from "../../shared/api";
import { toSummaryViewModel } from "./summaryModel";

describe("toSummaryViewModel", () => {
  it("maps completion response into display sections", () => {
    const model = toSummaryViewModel(completion());

    expect(model.sessionId).toBe("session-1");
    expect(model.evidenceCount).toBe(2);
    expect(model.sections.map((section) => section.title)).toEqual(["Highlights", "Memorable expressions", "Next focus"]);
  });

  it("omits empty sections", () => {
    const value = completion();
    value.dailySummary.memorableItems = [];

    expect(toSummaryViewModel(value).sections.map((section) => section.title)).toEqual(["Highlights", "Next focus"]);
  });
});

function completion(): TrainingSessionCompletion {
  return {
    session: {
      sessionId: "session-1",
      planId: "plan-1",
      type: "DAILY",
      mode: "TEXT",
      status: "COMPLETED",
      currentTaskId: "task-1",
      startedAt: "2026-08-10T00:00:00Z",
      completedAt: "2026-08-10T00:05:00Z",
      effectiveSeconds: 300,
    },
    dailySummary: {
      sessionId: "session-1",
      completedTaskCount: 1,
      evidenceCount: 2,
      practicedSkills: ["speaking"],
      highlights: ["You improved word choice."],
      memorableItems: ["really like"],
      nextFocus: ["Use natural adverbs before verbs."],
      generatedAt: "2026-08-10T00:05:00Z",
    },
  };
}
