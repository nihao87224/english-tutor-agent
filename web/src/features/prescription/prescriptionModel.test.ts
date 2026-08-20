import { describe, expect, it } from "vitest";
import { ApiError, type DailyLearningPrescription } from "../../shared/api";
import {
  buildRegenerationRequest,
  prescriptionFailed,
  prescriptionLoaded,
  primaryPrescriptionBlock,
  taskHeroStyle,
} from "./prescriptionModel";

describe("prescription model", () => {
  it("selects the ready output block as the primary Lin Muen scene", () => {
    const prescription = fixturePrescription();
    prescription.blocks.unshift({ ...prescription.blocks[0], blockId: "review", type: "REVIEW", sequence: 1 });

    expect(primaryPrescriptionBlock(prescription)?.blockId).toBe("output");
    expect(prescriptionLoaded(prescription)).toMatchObject({ status: "content" });
  });

  it("treats a prescription without ready blocks as empty", () => {
    const prescription = fixturePrescription();
    prescription.blocks[0].status = "SKIPPED";

    expect(prescriptionLoaded(prescription)).toEqual({ status: "empty" });
  });

  it("maps no-candidate, stale and API failures to explicit UI states", () => {
    const noCandidate = new ApiError("no candidate", 409, {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      code: "PRESCRIPTION_NO_CANDIDATE",
      fallbackAvailable: true,
    });
    const stale = new ApiError("stale", 409, {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      code: "PRESCRIPTION_STALE",
    });

    expect(prescriptionFailed(noCandidate)).toEqual({ status: "fallback", message: "no candidate" });
    expect(prescriptionFailed(stale, fixturePrescription())).toMatchObject({ status: "stale" });
    expect(prescriptionFailed(new Error("offline"))).toEqual({ status: "error", message: "offline" });
  });

  it("clamps focal points and preserves the API aspect ratio for responsive crop", () => {
    expect(
      taskHeroStyle({
        assetId: "hero",
        aspectRatio: "4:3",
        focalPoint: { x: 1.2, y: -0.1 },
        altText: "Lin Muen in a scene",
      }),
    ).toEqual({ objectPosition: "100% 0%", aspectRatio: "4 / 3" });
  });

  it("only sends available time for time feedback", () => {
    const prescription = fixturePrescription();
    expect(buildRegenerationRequest(prescription, "TIME_INSUFFICIENT", 10)).toMatchObject({
      currentPrescriptionId: "prx-airport",
      currentVersion: 3,
      reason: "TIME_INSUFFICIENT",
      availableMinutes: 10,
    });
    expect(buildRegenerationRequest(prescription, "TOO_HARD", 10).availableMinutes).toBeUndefined();
  });
});

function fixturePrescription(): DailyLearningPrescription {
  return {
    prescriptionId: "prx-airport",
    version: 3,
    learningDate: "2026-08-20",
    timezone: "Asia/Shanghai",
    status: "ACTIVE",
    priorityGoal: { code: "TRAVEL_COMMUNICATION", label: "Singapore travel communication" },
    rationale: "Confirming changed information is due for review.",
    reasonCodes: ["GOAL_MATCH", "REVIEW_DUE"],
    estimatedMinutes: 20,
    experience: { seasonId: "S01", episodeId: "EP006", sceneId: "airport_gate", title: "Airport Adventure" },
    blocks: [
      {
        blockId: "output",
        sequence: 2,
        type: "OUTPUT",
        title: "Confirm a changed gate",
        skillUnitVariantId: "travel.confirm_gate_change.b1",
        resource: { resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0" },
        episodeMappingId: "map-airport",
        difficulty: "B1",
        scaffolding: "MEDIUM",
        trainingType: "ROLE_PLAY",
        estimatedMinutes: 8,
        expectedEvidence: ["confirm_gate"],
        recommendationFactors: { goalMatch: 0.9 },
        taskHero: {
          assetId: "hero-airport",
          url: "https://cdn.example.test/airport.webp",
          aspectRatio: "16:9",
          focalPoint: { x: 0.68, y: 0.42 },
          altText: "Lin Muen stands full body beside an airport gate.",
        },
        status: "READY",
      },
    ],
    generatedAt: "2026-08-20T00:00:00Z",
    expiresAt: "2026-08-20T16:00:00Z",
  };
}
