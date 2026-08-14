import { beforeEach, describe, expect, it } from "vitest";
import {
  DEFAULT_ONBOARDING_STATE,
  loadOnboardingState,
  resetLocalSession,
  saveOnboardingState,
  type WebStorageLike,
} from "./localSession";

describe("localSession", () => {
  let storage: MemoryStorage;

  beforeEach(() => {
    storage = new MemoryStorage();
  });

  it("loads defaults when onboarding state is missing or invalid", () => {
    expect(loadOnboardingState(storage)).toEqual(DEFAULT_ONBOARDING_STATE);

    storage.setItem("englishTutor.web.onboarding", "{not-json");

    expect(loadOnboardingState(storage)).toEqual(DEFAULT_ONBOARDING_STATE);
  });

  it("normalizes and saves onboarding state", () => {
    saveOnboardingState(
      {
        completed: true,
        primaryGoal: "GENERAL",
        dailyMinutes: 20,
        correctionStyle: "LIGHT",
        saveRawText: false,
      },
      storage,
    );

    expect(loadOnboardingState(storage)).toEqual({
      completed: true,
      primaryGoal: "GENERAL",
      dailyMinutes: 20,
      correctionStyle: "LIGHT",
      saveRawText: false,
    });
  });

  it("clears local session keys", () => {
    saveOnboardingState(DEFAULT_ONBOARDING_STATE, storage);

    resetLocalSession(storage);

    expect(storage.getItem("englishTutor.web.onboarding")).toBeNull();
  });
});

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
