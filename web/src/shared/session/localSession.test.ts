import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  DEFAULT_ONBOARDING_STATE,
  getOrCreateUserKey,
  isValidUserKey,
  loadOnboardingState,
  resetLocalSession,
  saveOnboardingState,
  type WebStorageLike,
} from "./localSession";

describe("localSession", () => {
  let storage: MemoryStorage;

  beforeEach(() => {
    storage = new MemoryStorage();
    vi.spyOn(globalThis.crypto, "randomUUID").mockReturnValue("12345678-1234-4123-8123-123456789abc");
  });

  it("creates and reuses a valid local user key", () => {
    const created = getOrCreateUserKey(storage);
    const reused = getOrCreateUserKey(storage);

    expect(created).toBe("web_12345678123441238123123456789abc");
    expect(reused).toBe(created);
    expect(isValidUserKey(created)).toBe(true);
  });

  it("replaces invalid stored user keys", () => {
    storage.setItem("englishTutor.web.userKey", "bad key with spaces");

    expect(getOrCreateUserKey(storage)).toBe("web_12345678123441238123123456789abc");
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
    getOrCreateUserKey(storage);
    saveOnboardingState(DEFAULT_ONBOARDING_STATE, storage);

    resetLocalSession(storage);

    expect(storage.getItem("englishTutor.web.userKey")).toBeNull();
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
