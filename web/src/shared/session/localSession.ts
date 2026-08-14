import type { CorrectionStyle, PreferenceRequest, PrimaryGoal } from "../api";

const ONBOARDING_STORAGE_KEY = "englishTutor.web.onboarding";

export interface LocalOnboardingState {
  completed: boolean;
  primaryGoal: PrimaryGoal;
  dailyMinutes: PreferenceRequest["dailyMinutes"];
  correctionStyle: CorrectionStyle;
  saveRawText: boolean;
}

export const DEFAULT_ONBOARDING_STATE: LocalOnboardingState = {
  completed: false,
  primaryGoal: "WORKPLACE",
  dailyMinutes: 10,
  correctionStyle: "STANDARD",
  saveRawText: true,
};

export interface WebStorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export function loadOnboardingState(storage = getBrowserStorage()): LocalOnboardingState {
  const raw = storage?.getItem(ONBOARDING_STORAGE_KEY);
  if (!raw) {
    return DEFAULT_ONBOARDING_STATE;
  }

  try {
    return normalizeOnboardingState(JSON.parse(raw));
  } catch {
    return DEFAULT_ONBOARDING_STATE;
  }
}

export function saveOnboardingState(state: LocalOnboardingState, storage = getBrowserStorage()): void {
  storage?.setItem(ONBOARDING_STORAGE_KEY, JSON.stringify(normalizeOnboardingState(state)));
}

export function resetLocalSession(storage = getBrowserStorage()): void {
  storage?.removeItem(ONBOARDING_STORAGE_KEY);
}

function normalizeOnboardingState(value: unknown): LocalOnboardingState {
  if (!isObject(value)) {
    return DEFAULT_ONBOARDING_STATE;
  }

  return {
    completed: value.completed === true,
    primaryGoal: isPrimaryGoal(value.primaryGoal) ? value.primaryGoal : DEFAULT_ONBOARDING_STATE.primaryGoal,
    dailyMinutes: isDailyMinutes(value.dailyMinutes) ? value.dailyMinutes : DEFAULT_ONBOARDING_STATE.dailyMinutes,
    correctionStyle: isCorrectionStyle(value.correctionStyle)
      ? value.correctionStyle
      : DEFAULT_ONBOARDING_STATE.correctionStyle,
    saveRawText: typeof value.saveRawText === "boolean" ? value.saveRawText : DEFAULT_ONBOARDING_STATE.saveRawText,
  };
}

function getBrowserStorage(): WebStorageLike | undefined {
  return typeof window === "undefined" ? undefined : window.localStorage;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isPrimaryGoal(value: unknown): value is PrimaryGoal {
  return value === "WORKPLACE" || value === "GENERAL" || value === "IELTS";
}

function isCorrectionStyle(value: unknown): value is CorrectionStyle {
  return value === "LIGHT" || value === "STANDARD" || value === "STRICT";
}

function isDailyMinutes(value: unknown): value is PreferenceRequest["dailyMinutes"] {
  return value === 5 || value === 10 || value === 20 || value === 30 || value === 45;
}
