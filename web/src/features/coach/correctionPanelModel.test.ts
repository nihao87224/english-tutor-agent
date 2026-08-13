import { describe, expect, it } from "vitest";
import { toCorrectionPanelModel } from "./correctionPanelModel";

describe("toCorrectionPanelModel", () => {
  it("returns a waiting model before correction data arrives", () => {
    expect(toCorrectionPanelModel()).toMatchObject({ status: "waiting", items: [] });
  });

  it("returns no-error feedback when there are no corrections", () => {
    const model = toCorrectionPanelModel({
      hasError: false,
      corrections: [],
      overallFeedback: "Nice expression.",
      promptVersion: "p",
      schemaVersion: "s",
      traceId: "t",
      providerId: "fake",
      modelId: "fake",
    });

    expect(model.status).toBe("no-error");
    expect(model.overallFeedback).toBe("Nice expression.");
  });

  it("maps the first three corrections with natural expressions", () => {
    const model = toCorrectionPanelModel({
      hasError: true,
      corrections: Array.from({ length: 4 }, (_, index) => ({
        original: `old-${index}`,
        corrected: `new-${index}`,
        errorType: "word_choice",
        severity: "MEDIUM",
        explanationZh: "Use a more natural phrase.",
        shouldInterrupt: false,
        memoryWorthy: true,
        naturalSuggestions: [{ sentence: `natural-${index}`, style: "NEUTRAL" }],
      })),
      overallFeedback: "Focus on word choice.",
      promptVersion: "p",
      schemaVersion: "s",
      traceId: "t",
      providerId: "fake",
      modelId: "fake",
    });

    expect(model.status).toBe("has-corrections");
    expect(model.items).toHaveLength(3);
    expect(model.items[0]).toMatchObject({
      original: "old-0",
      corrected: "new-0",
      naturalExpressions: ["natural-0"],
      patternCue: "natural-0",
    });
    expect(model.tryAgainCue).toBe("natural-0");
  });
});
