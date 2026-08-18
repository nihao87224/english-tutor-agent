import { describe, expect, it } from "vitest";
import { resolveLearnerScreen } from "./userFlow";

describe("resolveLearnerScreen", () => {
  it.each([
    ["new user", "ONBOARDING_REQUIRED", "onboarding"],
    ["onboarding complete", "ASSESSMENT_REQUIRED", "assessment"],
    ["assessment complete", "READY_FOR_PLAN", "coach"],
    ["completed user", "READY_FOR_PLAN", "coach"],
  ] as const)("routes %s to %s", (_name, nextStep, expected) => {
    expect(resolveLearnerScreen({ nextStep, onboardingStep: "GOAL" })).toBe(expected);
  });
});
