import { describe, expect, it } from "vitest";
import {
  clearStoredAuthSession,
  loadStoredAuthSession,
  saveStoredAuthSession,
  toStoredAuthSession,
  type WebStorageLike,
} from "./authStore";

describe("authStore", () => {
  it("stores and loads an unexpired authenticated session", () => {
    const storage = new MemoryStorage();
    const session = toStoredAuthSession(
      {
        accessToken: "token",
        expiresIn: 60,
        user: {
          userKey: "user-1",
          email: "learner@example.com",
          status: "ACTIVE",
          roles: ["USER"],
          locale: "en",
          timezone: "UTC",
        },
      },
      1000,
    );

    saveStoredAuthSession(session, storage);

    expect(loadStoredAuthSession(storage, 1500)).toEqual(session);
  });

  it("clears expired or invalid sessions", () => {
    const storage = new MemoryStorage();
    storage.setItem("englishTutor.web.auth", JSON.stringify({ accessToken: "old", expiresAt: 10, user: { userKey: "u", email: "a@b.com" } }));

    expect(loadStoredAuthSession(storage, 11)).toBeNull();
    expect(storage.getItem("englishTutor.web.auth")).toBeNull();

    storage.setItem("englishTutor.web.auth", "{bad-json");
    expect(loadStoredAuthSession(storage, 0)).toBeNull();
  });

  it("clears stored auth session on logout", () => {
    const storage = new MemoryStorage();
    storage.setItem("englishTutor.web.auth", "value");

    clearStoredAuthSession(storage);

    expect(storage.getItem("englishTutor.web.auth")).toBeNull();
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
