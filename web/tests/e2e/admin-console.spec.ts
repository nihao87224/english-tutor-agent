import { expect, test, type Page } from "@playwright/test";

test("admin logs in, manages quota, provider secret, settings and audit", async ({ page }) => {
  await mockAdminBackend(page);

  await page.goto("/admin");

  await expect(page.getByRole("heading", { name: "Sign in to English Tutor" })).toBeVisible();
  await page.getByLabel("Email").fill("admin@example.com");
  await page.getByLabel("Password").fill("admin-password");
  await page.getByRole("button", { name: "Log in" }).last().click();

  await expect(page.getByRole("heading", { name: "Operations overview" })).toBeVisible();
  await expect(page.getByText("42")).toBeVisible();

  await page.getByRole("button", { name: "US Users" }).click();
  await expect(page.getByText("learner@example.com")).toBeVisible();
  await page.getByRole("button", { name: "Manage" }).click();
  await expect(page.getByLabel("User management drawer")).toBeVisible();
  await page.getByLabel("Daily quota override").fill("30");
  await page.getByRole("button", { name: "Save quota policy" }).click();
  await expect(page.getByText("Quota policy saved.")).toBeVisible();
  await expect(page.getByText("Remaining")).toBeVisible();

  await page.getByLabel("Temporary quota bonus").fill("5");
  await page.getByRole("button", { name: "Add bonus" }).click();
  await expect(page.getByText("Temporary quota bonus added.")).toBeVisible();
  await page.getByRole("button", { name: "Reset today's usage" }).click();
  await expect(page.getByText("Today's usage has been reset.")).toBeVisible();
  await page.getByRole("button", { name: "Back" }).click();

  await page.getByRole("button", { name: "PR AI Providers" }).click();
  await expect(page.getByRole("heading", { name: "AI Providers" })).toBeVisible();
  await page.getByLabel("API key").fill("sk-test-secret");
  await page.getByRole("button", { name: "Replace secret" }).click();
  await expect(page.getByText("API key replaced and kept masked.")).toBeVisible();
  await expect(page.getByText("Secret is configured; the raw value is never displayed.")).toBeVisible();

  await page.getByRole("button", { name: "SE Settings" }).click();
  await expect(page.getByRole("heading", { name: "System settings" })).toBeVisible();
  await page.locator(".setting-row-editor input").nth(0).fill("60");
  await page.getByRole("button", { name: "Save" }).first().click();
  await expect(page.getByText("System setting saved.")).toBeVisible();

  await page.getByRole("button", { name: "AU Audit" }).click();
  await expect(page.getByRole("heading", { name: "Audit log" })).toBeVisible();
  await expect(page.getByText("USER_QUOTA_POLICY_UPDATED")).toBeVisible();

  await page.getByLabel("Language").selectOption("zh-CN");
  await expect(page.getByText("审计日志").first()).toBeVisible();

  await page.setViewportSize({ width: 390, height: 820 });
  await expect(page.getByRole("button", { name: "AU 审计日志" })).toBeVisible();
});

test("learner account is blocked from admin route", async ({ page }) => {
  await mockLearnerBackend(page);

  await page.goto("/admin");
  await page.getByLabel("Email").fill("learner@example.com");
  await page.getByLabel("Password").fill("learner-password");
  await page.getByRole("button", { name: "Log in" }).last().click();

  await expect(page.getByRole("heading", { name: "This account cannot access admin" })).toBeVisible();
  await page.getByRole("button", { name: "Open learner app" }).click();
  await expect(page).toHaveURL("/");
});

test("admin receives visible feedback when a sensitive change fails", async ({ page }) => {
  await mockAdminBackend(page);
  await page.route("http://localhost:8080/api/v1/admin/ai-providers/openai/secret", async (route) => {
    await route.fulfill({
      status: 500,
      contentType: "application/problem+json",
      body: JSON.stringify({ type: "about:blank", title: "Server error", status: 500 }),
    });
  });

  await page.goto("/admin");
  await page.getByLabel("Email").fill("admin@example.com");
  await page.getByLabel("Password").fill("admin-password");
  await page.getByRole("button", { name: "Log in" }).last().click();
  await page.getByRole("button", { name: "PR AI Providers" }).click();
  await page.getByLabel("API key").fill("sk-test-secret");
  await page.getByRole("button", { name: "Replace secret" }).click();

  await expect(page.getByText("Admin data is unavailable. Please retry.")).toBeVisible();
});

async function mockAdminBackend(page: Page) {
  const adminUser = {
    userKey: "admin-user",
    email: "admin@example.com",
    status: "ACTIVE",
    roles: ["ADMIN"],
    locale: "en",
    timezone: "UTC",
  };

  const learnerSummary = {
    userId: 2,
    userKey: "learner-user",
    email: "learner@example.com",
    status: "ACTIVE",
    roles: ["USER"],
    createdAt: "2026-08-10T00:00:00Z",
    lastLoginAt: "2026-08-10T02:00:00Z",
  };

  const learnerDetail = {
    ...learnerSummary,
    locale: "en",
    timezone: "UTC",
    authVersion: 1,
    authorities: ["LEARNER"],
    updatedAt: "2026-08-10T01:00:00Z",
    disabledAt: null,
  };

  let provider = {
    providerCode: "openai",
    providerType: "OPENAI",
    displayName: "OpenAI",
    enabled: true,
    defaultLlm: true,
    defaultAsr: true,
    defaultTts: true,
    baseUrl: "https://api.openai.com/v1",
    llmModel: "gpt-4.1-mini",
    asrModel: "gpt-4o-mini-transcribe",
    ttsModel: "gpt-4o-mini-tts",
    ttsVoice: "alloy",
    timeoutSeconds: 30,
    apiKeyConfigured: false,
    apiKeyMaskedHint: null,
  };

  await page.route("http://localhost:8080/api/v1/auth/refresh", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/problem+json",
      body: JSON.stringify({ type: "about:blank", title: "Unauthorized", status: 401 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: { "set-cookie": "ETA_REFRESH_TOKEN=refresh; Path=/api/v1/auth; HttpOnly; SameSite=Lax" },
      body: JSON.stringify({ user: adminUser, accessToken: "admin-token", expiresIn: 3600 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/admin/dashboard", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        totalUsers: 42,
        activeUsersToday: 8,
        newUsersToday: 3,
        aiRequestsToday: 19,
        usersReachedQuotaLimit: 2,
        activeDefaultProvider: "openai",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/admin/users**", async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname === "/api/v1/admin/users/learner-user") {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(learnerDetail) });
      return;
    }
    if (pathname !== "/api/v1/admin/users") {
      await route.fallback();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [learnerSummary], page: 0, size: 20, total: 1 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/admin/users/learner-user/quota-policy", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(quotaState(30, 0, 0)) });
  });

  await page.route("http://localhost:8080/api/v1/admin/users/learner-user/quota/bonus", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(quotaState(30, 0, 5)) });
  });

  await page.route("http://localhost:8080/api/v1/admin/users/learner-user/quota/reset-today", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(quotaState(30, 0, 5)) });
  });

  await page.route("http://localhost:8080/api/v1/admin/ai-providers", async (route) => {
    if (new URL(route.request().url()).pathname !== "/api/v1/admin/ai-providers") {
      await route.fallback();
      return;
    }
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([provider]) });
  });

  await page.route("http://localhost:8080/api/v1/admin/ai-providers/openai", async (route) => {
    if (route.request().method() === "PUT") {
      provider = { ...provider, ...(await route.request().postDataJSON()), providerCode: "openai", apiKeyConfigured: provider.apiKeyConfigured };
    }
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(provider) });
  });

  await page.route("http://localhost:8080/api/v1/admin/ai-providers/openai/secret", async (route) => {
    provider = { ...provider, apiKeyConfigured: true, apiKeyMaskedHint: "sk-...test" };
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(provider) });
  });

  await page.route("http://localhost:8080/api/v1/admin/settings", async (route) => {
    if (new URL(route.request().url()).pathname !== "/api/v1/admin/settings") {
      await route.fallback();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        {
          key: "quota.default_daily_limit",
          value: "50",
          valueType: "INTEGER",
          description: "Default daily quota",
          updatedAt: "2026-08-10T00:00:00Z",
        },
      ]),
    });
  });

  await page.route("http://localhost:8080/api/v1/admin/settings/quota.default_daily_limit", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        key: "quota.default_daily_limit",
        value: "60",
        valueType: "INTEGER",
        description: "Default daily quota",
        updatedAt: "2026-08-10T03:00:00Z",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/admin/audit**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        items: [
          {
            id: 1,
            actorUserId: 1,
            actorEmail: "admin@example.com",
            actionCode: "USER_QUOTA_POLICY_UPDATED",
            targetType: "USER",
            targetKey: "learner-user",
            createdAt: "2026-08-10T04:00:00Z",
          },
        ],
        page: 0,
        size: 20,
        total: 1,
      }),
    });
  });
}

async function mockLearnerBackend(page: Page) {
  await page.route("http://localhost:8080/api/v1/auth/refresh", async (route) => {
    await route.fulfill({ status: 401, contentType: "application/problem+json", body: JSON.stringify({ type: "about:blank", title: "Unauthorized", status: 401 }) });
  });
  await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        user: {
          userKey: "learner-user",
          email: "learner@example.com",
          status: "ACTIVE",
          roles: ["USER"],
          locale: "en",
          timezone: "UTC",
        },
        accessToken: "learner-token",
        expiresIn: 3600,
      }),
    });
  });
}

function quotaState(dailyLimit: number, used: number, bonus: number) {
  return {
    userKey: "learner-user",
    dailyLimitOverride: dailyLimit,
    unlimited: false,
    quotaDate: "2026-08-10",
    dailyLimit,
    used,
    reserved: 0,
    bonus,
    remaining: dailyLimit + bonus - used,
  };
}
