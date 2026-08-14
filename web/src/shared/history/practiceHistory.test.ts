import { describe, expect, it } from "vitest";
import { loadPracticeHistory, savePracticeCompletion, type WebStorageLike } from "./practiceHistory";
import type { TrainingSessionCompletion } from "../api";

describe("practiceHistory", () => {
  it("saves recent completions by normalized email", () => {
    const storage = new MemoryStorage();
    const completion = completionFor("session-1");

    const saved = savePracticeCompletion("Learner@Example.com", completion, storage);

    expect(saved).toHaveLength(1);
    expect(loadPracticeHistory("learner@example.com", storage)[0]?.sessionId).toBe("session-1");
    expect(loadPracticeHistory("other@example.com", storage)).toEqual([]);
  });

  it("replaces duplicate sessions and ignores corrupt stored data", () => {
    const storage = new MemoryStorage();
    savePracticeCompletion("learner@example.com", completionFor("session-1", "first"), storage);
    savePracticeCompletion("learner@example.com", completionFor("session-1", "second"), storage);

    expect(loadPracticeHistory("learner@example.com", storage)).toHaveLength(1);
    expect(loadPracticeHistory("learner@example.com", storage)[0]?.highlights).toEqual(["second"]);

    storage.setItem("englishTutor.web.practiceHistory.learner@example.com", "{bad-json");
    expect(loadPracticeHistory("learner@example.com", storage)).toEqual([]);
  });
});

function completionFor(sessionId: string, highlight = "highlight"): TrainingSessionCompletion {
  return {
    session: {
      sessionId,
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
      sessionId,
      completedTaskCount: 1,
      evidenceCount: 2,
      practicedSkills: ["expression"],
      highlights: [highlight],
      memorableItems: ["really like"],
      nextFocus: ["Use natural adverbs before verbs."],
      generatedAt: "2026-08-10T00:05:00Z",
    },
  };
}

class MemoryStorage implements WebStorageLike {
  private readonly values = new Map<string, string>();

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }
}
