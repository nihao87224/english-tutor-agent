import type { TrainingSessionCompletion } from "../api";

const HISTORY_KEY_PREFIX = "englishTutor.web.practiceHistory.";
const MAX_HISTORY_ITEMS = 10;

export interface PracticeHistoryItem {
  sessionId: string;
  completedAt: string;
  completedTaskCount: number;
  evidenceCount: number;
  highlights: string[];
  memorableItems: string[];
  nextFocus: string[];
}

export interface WebStorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export function loadPracticeHistory(email: string, storage = getBrowserStorage()): PracticeHistoryItem[] {
  const raw = storage?.getItem(historyKey(email));
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map(normalizeItem).filter((item): item is PracticeHistoryItem => item !== null) : [];
  } catch {
    return [];
  }
}

export function savePracticeCompletion(
  email: string,
  completion: TrainingSessionCompletion,
  storage = getBrowserStorage(),
): PracticeHistoryItem[] {
  const item: PracticeHistoryItem = {
    sessionId: completion.session.sessionId,
    completedAt: completion.session.completedAt ?? completion.dailySummary.generatedAt,
    completedTaskCount: completion.dailySummary.completedTaskCount,
    evidenceCount: completion.dailySummary.evidenceCount,
    highlights: completion.dailySummary.highlights,
    memorableItems: completion.dailySummary.memorableItems,
    nextFocus: completion.dailySummary.nextFocus,
  };
  const next = [item, ...loadPracticeHistory(email, storage).filter((existing) => existing.sessionId !== item.sessionId)].slice(
    0,
    MAX_HISTORY_ITEMS,
  );
  storage?.setItem(historyKey(email), JSON.stringify(next));
  return next;
}

function normalizeItem(value: unknown): PracticeHistoryItem | null {
  if (!isObject(value) || typeof value.sessionId !== "string" || typeof value.completedAt !== "string") {
    return null;
  }
  return {
    sessionId: value.sessionId,
    completedAt: value.completedAt,
    completedTaskCount: typeof value.completedTaskCount === "number" ? value.completedTaskCount : 0,
    evidenceCount: typeof value.evidenceCount === "number" ? value.evidenceCount : 0,
    highlights: stringArray(value.highlights),
    memorableItems: stringArray(value.memorableItems),
    nextFocus: stringArray(value.nextFocus),
  };
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}

function historyKey(email: string): string {
  return `${HISTORY_KEY_PREFIX}${email.trim().toLowerCase()}`;
}

function getBrowserStorage(): WebStorageLike | undefined {
  return typeof window === "undefined" ? undefined : window.localStorage;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
