import { expect, test, type Page, type Route } from "@playwright/test";

test("shows a responsive Lin Muen prescription and recomposes it from keyboard feedback", async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await seedAuthenticatedLearner(page);
  await mockPrescriptionBackend(page);

  await page.goto("/");

  await expect(page.getByRole("heading", { name: "确认并复述机场信息" })).toBeVisible();
  const airportHero = page.getByRole("img", { name: "Lin Muen 站在机场登机口旁，准备确认变更信息。" });
  await expect(airportHero).toBeVisible();
  await expect(airportHero).toHaveCSS("object-position", "68% 42%");

  const tooHard = page.getByRole("button", { name: "太难了" });
  await tooHard.focus();
  await expect(tooHard).toBeFocused();
  await page.keyboard.press("Enter");

  await expect(page.getByRole("heading", { name: "礼貌提出咖啡店请求" })).toBeVisible();
  await expect(page.getByRole("img", { name: "Lin Muen 站在咖啡店柜台前提出燕麦奶请求。" })).toBeVisible();
  await expect(page.getByText("已根据你的反馈更新处方。")).toBeVisible();

  const dimensions = await page.evaluate(() => ({ viewport: window.innerWidth, content: document.documentElement.scrollWidth }));
  expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport);
  await expect(page.locator(".vite-error-overlay")).toHaveCount(0);
  expect(await page.locator("body").innerText()).not.toHaveLength(0);
  expect(consoleErrors).toEqual([]);
  await page.screenshot({ path: "test-results/today-prescription-mobile.png", fullPage: true });
});

async function seedAuthenticatedLearner(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem("englishTutor.web.locale", "zh-CN");
    window.localStorage.setItem("englishTutor.web.auth", JSON.stringify({
      user: {
        userKey: "learner-v2",
        email: "learner-v2@example.com",
        status: "ACTIVE",
        roles: ["USER"],
        locale: "zh-CN",
        timezone: "Asia/Shanghai",
      },
      accessToken: "v2-access-token",
      expiresAt: Date.now() + 3_600_000,
    }));
  });
}

async function mockPrescriptionBackend(page: Page) {
  await page.route("**/api/v1/users/me/progress", (route) => json(route, {
    nextStep: "READY_FOR_PLAN",
    onboardingStep: "COMPLETE",
  }));
  await page.route("**/api/v1/me/quota", (route) => json(route, {
    userKey: "learner-v2",
    quotaDate: "2026-08-20",
    dailyLimit: 20,
    used: 2,
    reserved: 0,
    bonus: 0,
    remaining: 18,
    unlimited: false,
    resetAt: "2026-08-21T00:00:00Z",
  }));
  await page.route("**/api/v1/prescriptions/today?**", async (route) => {
    if (route.request().method() === "POST") {
      const request = await route.request().postDataJSON();
      expect(request).toMatchObject({
        currentPrescriptionId: "prx-airport",
        currentVersion: 1,
        reason: "TOO_HARD",
      });
      expect(route.request().headers()["idempotency-key"]).toBeTruthy();
      await json(route, coffeePrescription(), 201);
      return;
    }
    await json(route, airportPrescription());
  });
  await page.route("**/api/v1/prescriptions/today/regenerations", async (route) => {
    const request = await route.request().postDataJSON();
    expect(request).toMatchObject({ currentPrescriptionId: "prx-airport", currentVersion: 1, reason: "TOO_HARD" });
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    await json(route, coffeePrescription(), 201);
  });
}

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

function airportPrescription() {
  return prescription({
    prescriptionId: "prx-airport",
    version: 1,
    goal: "确认并复述机场信息",
    title: "确认新的登机口",
    episodeId: "EP006",
    sceneId: "airport_gate",
    experienceTitle: "Airport Adventure",
    assetId: "airport-hero",
    altText: "Lin Muen 站在机场登机口旁，准备确认变更信息。",
    focalPoint: { x: 0.68, y: 0.42 },
  });
}

function coffeePrescription() {
  return prescription({
    prescriptionId: "prx-coffee",
    version: 2,
    goal: "礼貌提出咖啡店请求",
    title: "提出替换燕麦奶的请求",
    episodeId: "EP003",
    sceneId: "coffee_counter",
    experienceTitle: "Coffee Stop",
    assetId: "coffee-hero",
    altText: "Lin Muen 站在咖啡店柜台前提出燕麦奶请求。",
    focalPoint: { x: 0.35, y: 0.45 },
  });
}

function prescription(input: {
  prescriptionId: string;
  version: number;
  goal: string;
  title: string;
  episodeId: string;
  sceneId: string;
  experienceTitle: string;
  assetId: string;
  altText: string;
  focalPoint: { x: number; y: number };
}) {
  return {
    prescriptionId: input.prescriptionId,
    version: input.version,
    learningDate: "2026-08-20",
    timezone: "Asia/Shanghai",
    status: "ACTIVE",
    priorityGoal: { code: "PERSONALIZED_GOAL", label: input.goal },
    rationale: "根据你的目标、最近短板和到期复习生成。",
    reasonCodes: ["GOAL_MATCH", "REVIEW_DUE"],
    estimatedMinutes: 15,
    experience: { seasonId: "S01", episodeId: input.episodeId, sceneId: input.sceneId, title: input.experienceTitle },
    blocks: [{
      blockId: `block-${input.assetId}`,
      sequence: 1,
      type: "OUTPUT",
      title: input.title,
      skillUnitVariantId: input.sceneId,
      resource: { resourceId: `resource-${input.assetId}`, resourceVersion: "1.0.0" },
      episodeMappingId: `mapping-${input.assetId}`,
      difficulty: "B1",
      scaffolding: "MEDIUM",
      trainingType: "ROLE_PLAY",
      estimatedMinutes: 8,
      expectedEvidence: ["complete_communication_goal"],
      recommendationFactors: { goalMatch: 0.9 },
      taskHero: {
        assetId: input.assetId,
        url: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1600' height='900'%3E%3Crect width='1600' height='900' fill='%239fc8bb'/%3E%3C/svg%3E",
        aspectRatio: "16:9",
        focalPoint: input.focalPoint,
        altText: input.altText,
      },
      status: "READY",
    }],
    generatedAt: "2026-08-20T00:00:00Z",
    expiresAt: "2026-08-20T16:00:00Z",
  };
}
