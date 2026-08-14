import type { AuthResponse, AuthUser } from "../api";

const AUTH_STORAGE_KEY = "englishTutor.web.auth";

export interface StoredAuthSession {
  user: AuthUser;
  accessToken: string;
  expiresAt: number;
}

export interface WebStorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export function toStoredAuthSession(response: AuthResponse, now = Date.now()): StoredAuthSession {
  return {
    user: response.user,
    accessToken: response.accessToken,
    expiresAt: now + response.expiresIn * 1000,
  };
}

export function loadStoredAuthSession(storage = getBrowserStorage(), now = Date.now()): StoredAuthSession | null {
  const raw = storage?.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = normalizeStoredAuthSession(JSON.parse(raw));
    if (!session || session.expiresAt <= now) {
      storage?.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    storage?.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveStoredAuthSession(session: StoredAuthSession, storage = getBrowserStorage()): void {
  storage?.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredAuthSession(storage = getBrowserStorage()): void {
  storage?.removeItem(AUTH_STORAGE_KEY);
}

function normalizeStoredAuthSession(value: unknown): StoredAuthSession | null {
  if (!isObject(value) || !isObject(value.user)) {
    return null;
  }
  if (typeof value.accessToken !== "string" || value.accessToken.length === 0) {
    return null;
  }
  if (typeof value.expiresAt !== "number" || !Number.isFinite(value.expiresAt)) {
    return null;
  }
  const user = value.user;
  if (typeof user.userKey !== "string" || typeof user.email !== "string") {
    return null;
  }
  return {
    accessToken: value.accessToken,
    expiresAt: value.expiresAt,
    user: {
      userKey: user.userKey,
      email: user.email,
      status: typeof user.status === "string" ? user.status : "ACTIVE",
      roles: Array.isArray(user.roles) ? user.roles.filter((role): role is string => typeof role === "string") : [],
      locale: typeof user.locale === "string" ? user.locale : "zh-CN",
      timezone: typeof user.timezone === "string" ? user.timezone : "Asia/Shanghai",
    },
  };
}

function getBrowserStorage(): WebStorageLike | undefined {
  return typeof window === "undefined" ? undefined : window.localStorage;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
