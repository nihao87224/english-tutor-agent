import { expect, test, type Page, type Route } from "@playwright/test";

test("starts and resumes the responsive Lin Muen airport scenario with hidden-first transcript", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await seedAuthenticatedLearner(page);
  let session = lessonSession("SCENE_CONTEXT");
  await mockBackend(page, () => session, (next) => { session = next; });

  await page.goto("/");
  await page.getByRole("button", { name: "进入场景课程" }).click();

  await expect(page).toHaveURL(/\/lesson-sessions\/lesson-session-ep006$/);
  const taskHero = page.getByRole("img", { name: /Lin Muen stands full body near an airport boarding gate/ });
  await expect(taskHero).toBeVisible();
  await expect(taskHero).toHaveCSS("object-position", "62% 48%");
  await expect(page.getByText("Help Lin Muen confirm the new gate and find out when boarding starts.")).toBeVisible();

  await page.getByRole("button", { name: /我了解场景了/ }).click();
  await expect(page.getByRole("heading", { name: "先听一遍，不急着看文字" })).toBeVisible();
  const transcriptButton = page.getByRole("button", { name: "展开 Transcript 与 Expressions" });
  await expect(transcriptButton).toHaveAttribute("aria-expanded", "false");
  await expect(page.getByText("Could you help me check?", { exact: true })).toHaveCount(0);

  await page.reload();
  await expect(page.getByRole("heading", { name: "先听一遍，不急着看文字" })).toBeVisible();
  await transcriptButton.click();
  await expect(page.getByText("Could you help me check?", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("你能帮我确认一下吗？")).toBeVisible();

  session = lessonSession("GUIDED_SPEAKING");
  await page.reload();
  await expect(taskHero).toBeVisible();
  await expect(page.getByText("Speaking 训练区会继续保留当前 Lin Muen 场景图").first()).toBeVisible();

  const dimensions = await page.evaluate(() => ({ viewport: window.innerWidth, content: document.documentElement.scrollWidth }));
  expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport);
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

async function mockBackend(
  page: Page,
  current: () => ReturnType<typeof lessonSession>,
  update: (session: ReturnType<typeof lessonSession>) => void,
) {
  await page.route("**/api/v1/users/me/progress", (route) => json(route, { nextStep: "READY_FOR_PLAN", onboardingStep: "COMPLETE" }));
  await page.route("**/api/v1/me/quota", (route) => json(route, {
    quotaDate: "2026-08-20", dailyLimit: 20, used: 2, bonus: 0, remaining: 18, unlimited: false,
    resetAt: "2026-08-21T00:00:00Z",
  }));
  await page.route("**/api/v1/prescriptions/today?**", (route) => json(route, prescription()));
  await page.route("**/api/v1/lesson-sessions", async (route) => {
    expect(route.request().method()).toBe("POST");
    expect(await route.request().postDataJSON()).toEqual({
      prescriptionId: "prx-airport", prescriptionVersion: 3, blockId: "output", inputMode: "VOICE_OR_TEXT",
    });
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    await json(route, current(), 201);
  });
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006", (route) => json(route, current()));
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006/steps/SCENE_CONTEXT/completions", async (route) => {
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    const next = lessonSession("FIRST_LISTEN");
    update(next);
    await json(route, next);
  });
  await page.route("**/api/v1/learning-resources/season1.ep006.gate_change.b1/versions/1.0.0", (route) => json(route, learningResource()));
  await page.route("**/api/v1/learning-resources/season1.ep006.gate_change.b1/media-access", async (route) => {
    const request = await route.request().postDataJSON();
    const image = request.purpose === "DISPLAY";
    await json(route, {
      assetId: request.assetId,
      url: image
        ? "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1600' height='900'%3E%3Crect width='1600' height='900' fill='%235f887b'/%3E%3C/svg%3E"
        : "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=",
      mimeType: image ? "image/svg+xml" : "audio/wav",
      contentHash: image ? "sha256:hero" : "sha256:audio",
    });
  });
}

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

function lessonSession(step: "SCENE_CONTEXT" | "FIRST_LISTEN" | "GUIDED_SPEAKING") {
  const clientCompletable = step !== "GUIDED_SPEAKING";
  return {
    sessionId: "lesson-session-ep006",
    prescriptionId: "prx-airport",
    prescriptionVersion: 3,
    blockId: "output",
    status: "IN_PROGRESS",
    resource: { resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0" },
    skillUnitVariantId: "travel.confirm_gate_change.b1",
    episodeMappingId: "map-airport",
    inputMode: "VOICE_OR_TEXT",
    currentStep: step,
    step: { stepId: step, completionMode: clientCompletable ? "CLIENT_ACKNOWLEDGEMENT" : "ATTEMPT_REQUIRED", clientCompletable },
    progress: { completedSteps: step === "SCENE_CONTEXT" ? 0 : 1, totalRequiredSteps: 9 },
    version: step === "SCENE_CONTEXT" ? 1 : 2,
  };
}

function prescription() {
  return {
    prescriptionId: "prx-airport", version: 3, learningDate: "2026-08-20", timezone: "Asia/Shanghai", status: "ACTIVE",
    priorityGoal: { code: "TRAVEL", label: "确认并复述机场信息" },
    rationale: "旅行目标优先，而且确认信息技能今天到期复习。", reasonCodes: ["GOAL_MATCH"], estimatedMinutes: 15,
    experience: { seasonId: "S01", episodeId: "EP006", sceneId: "GATE_CHANGE", title: "Airport Adventure" },
    blocks: [{
      blockId: "output", sequence: 1, type: "OUTPUT", title: "确认新的登机口",
      skillUnitVariantId: "travel.confirm_gate_change.b1",
      resource: { resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0" },
      episodeMappingId: "map-airport", difficulty: "B1", scaffolding: "MEDIUM", trainingType: "ROLE_PLAY",
      estimatedMinutes: 8, expectedEvidence: ["confirm_gate"], recommendationFactors: { goalMatch: 0.9 },
      taskHero: {
        assetId: "hero-airport",
        url: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1600' height='900'%3E%3Crect width='1600' height='900' fill='%235f887b'/%3E%3C/svg%3E",
        aspectRatio: "16:9", focalPoint: { x: 0.62, y: 0.48 },
        altText: "Lin Muen stands full body near an airport boarding gate and checks changed flight details.",
      },
      status: "READY",
    }],
    generatedAt: "2026-08-20T00:00:00Z", expiresAt: "2026-08-20T16:00:00Z",
  };
}

function learningResource() {
  return {
    resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0", collectionId: "INTERNAL_SCENARIO_LIBRARY",
    providerCode: "english-tutor-agent", type: "SCENARIO_LESSON", title: "Confirm a Gate Change with Lin Muen",
    description: "Airport scenario", language: "en", level: "B1", topic: "Travel", scene: "GATE_CHANGE",
    communicationGoal: "Confirm a changed boarding gate and the next action clearly", accessScope: "PUBLIC",
    publishStatus: "PUBLISHED", estimatedMinutes: 12, skillUnitVariantIds: ["travel.confirm_gate_change.b1"],
    taskHero: {
      assetId: "task-hero", assetVersion: "1.0.0", mediaType: "IMAGE", purpose: "TASK_HERO", accessScope: "PUBLIC",
      mimeType: "image/webp", contentHash: "sha256:hero", byteLength: 1234,
      metadata: {
        aspectRatio: "16:9", shotType: "environmental_full_body",
        displaySurfaces: ["prescription_card", "scenario_intro", "scenario_training"], focalPoint: { x: 0.62, y: 0.48 },
        altText: "Lin Muen stands full body near an airport boarding gate and checks changed flight details.",
      },
    },
    audioAssets: [{
      assetId: "scene-audio", assetVersion: "1.0.0", mediaType: "AUDIO", purpose: "SCENE_DIALOGUE", accessScope: "PUBLIC",
      mimeType: "audio/mpeg", contentHash: "sha256:audio", byteLength: 2345, metadata: {},
    }],
    learnerFit: {},
    content: { lessonPackage: {
      character: "Lin Muen", seasonId: "S01", episodeId: "EP006",
      story: {
        title: "The Gate Has Changed",
        context: "Lin Muen is waiting in the airport departure area when she sees that her gate may have changed.",
        mission: "Help Lin Muen confirm the new gate and find out when boarding starts.",
      },
      transcript: { sentences: [
        { sentenceId: "gate-001", speaker: "Lin Muen", text: "Could you help me check?" },
        { sentenceId: "gate-002", speaker: "Airport Agent", text: "Your flight now departs from Gate 24." },
      ] },
      expressions: [{ expression: "Could you help me check?", meaningZh: "你能帮我确认一下吗？", usage: "Polite request" }],
    } },
    assets: [], publishedAt: "2026-08-20T00:00:00Z",
  };
}
