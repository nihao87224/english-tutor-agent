import { describe, expect, it } from "vitest";
import { adminPathForView, adminViewFromPath, hasAdminRole, providerToUpdateRequest, quotaUsagePercent } from "./AdminConsole";

describe("AdminConsole helpers", () => {
  it("maps admin paths to stable console views", () => {
    expect(adminViewFromPath("/admin")).toBe("overview");
    expect(adminViewFromPath("/admin/users")).toBe("users");
    expect(adminViewFromPath("/admin/providers/openai")).toBe("providers");
    expect(adminViewFromPath("/admin/settings")).toBe("settings");
    expect(adminViewFromPath("/admin/audit")).toBe("audit");
    expect(adminPathForView("users")).toBe("/admin/users");
  });

  it("checks admin roles case-insensitively", () => {
    expect(hasAdminRole({ roles: ["USER"] })).toBe(false);
    expect(hasAdminRole({ roles: ["user", "admin"] })).toBe(true);
  });

  it("calculates quota usage without dividing by zero or capping over 100", () => {
    expect(
      quotaUsagePercent({
        userKey: "user-1",
        unlimited: false,
        quotaDate: "2026-08-10",
        dailyLimit: 10,
        used: 5,
        reserved: 0,
        bonus: 0,
        remaining: 5,
      }),
    ).toBe(50);
    expect(
      quotaUsagePercent({
        userKey: "user-1",
        unlimited: false,
        quotaDate: "2026-08-10",
        dailyLimit: 0,
        used: 2,
        reserved: 0,
        bonus: 0,
        remaining: 0,
      }),
    ).toBe(100);
    expect(
      quotaUsagePercent({
        userKey: "user-1",
        unlimited: true,
        quotaDate: "2026-08-10",
        dailyLimit: 0,
        used: 20,
        reserved: 0,
        bonus: 0,
        remaining: 0,
      }),
    ).toBe(0);
  });

  it("builds provider update requests without carrying masked secrets", () => {
    expect(
      providerToUpdateRequest({
        providerCode: "openai",
        providerType: "OPENAI",
        displayName: "OpenAI",
        enabled: true,
        defaultLlm: true,
        defaultAsr: false,
        defaultTts: false,
        baseUrl: "https://api.openai.com/v1",
        llmModel: "gpt-4.1-mini",
        asrModel: "gpt-4o-mini-transcribe",
        ttsModel: "gpt-4o-mini-tts",
        ttsVoice: "alloy",
        timeoutSeconds: 30,
        apiKeyConfigured: true,
        apiKeyMaskedHint: "sk-...abcd",
      }),
    ).toEqual({
      providerType: "OPENAI",
      displayName: "OpenAI",
      enabled: true,
      defaultLlm: true,
      defaultAsr: false,
      defaultTts: false,
      baseUrl: "https://api.openai.com/v1",
      llmModel: "gpt-4.1-mini",
      asrModel: "gpt-4o-mini-transcribe",
      ttsModel: "gpt-4o-mini-tts",
      ttsVoice: "alloy",
      timeoutSeconds: 30,
    });
  });
});
