import { expect, test, type Page, type Route } from "@playwright/test";

test("starts and resumes the responsive Lin Muen airport scenario with hidden-first transcript", async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") consoleErrors.push(message.text()); });
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

  session = lessonSession("COMPREHENSION");
  await page.reload();
  await expect(page.getByText("Which gate should Lin Muen use?")).toBeVisible();
  await page.getByLabel("你的答案").fill("Gate 24.");
  await page.getByRole("button", { name: "提交理解答案" }).click();
  await expect(page.getByText("When does boarding begin?")).toBeVisible();
  await page.getByLabel("你的答案").fill("3:20");
  await page.getByRole("button", { name: "提交理解答案" }).click();
  await expect(page.getByRole("heading", { name: "对照文本，提取可复用表达" })).toBeVisible();

  session = lessonSession("GUIDED_SPEAKING");
  await page.reload();
  await expect(taskHero).toBeVisible();
  await expect(page.getByRole("heading", { name: "把关键信息说给 Lin Muen" })).toBeVisible();
  await page.getByLabel("用英文组织你的回答").fill("Your flight leaves from Gate 24. Boarding begins at 3:20.");
  await page.getByRole("button", { name: "提交口语文本" }).click();
  await expect(page.getByRole("heading", { name: "和 Lin Muen 一起完成真实对话" })).toBeVisible();

  const dimensions = await page.evaluate(() => ({ viewport: window.innerWidth, content: document.documentElement.scrollWidth }));
  expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport);
  await expect(page.locator("vite-error-overlay, #webpack-dev-server-client-overlay, [data-nextjs-dialog]")).toHaveCount(0);
  expect(consoleErrors).toEqual([]);
});

test("records half-duplex audio and blocks low-confidence ASR until correction", async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") consoleErrors.push(message.text()); });
  await seedAuthenticatedLearner(page);
  await page.addInitScript(() => {
    Object.defineProperty(navigator, "mediaDevices", { value: {
      getUserMedia: async () => ({ getTracks: () => [{ stop() {} }] }),
    } });
    (window as any).MediaRecorder = class {
      static isTypeSupported() { return true; }
      state = "inactive";
      mimeType = "audio/webm;codecs=opus";
      ondataavailable?: (event: { data: Blob }) => void;
      onstop?: () => void;
      start() { this.state = "recording"; }
      stop() {
        this.state = "inactive";
        this.ondataavailable?.({ data: new Blob(["voice"], { type: this.mimeType }) });
        this.onstop?.();
      }
    };
  });
  let session = lessonSession("GUIDED_SPEAKING");
  await mockBackend(page, () => session, (next) => { session = next; });

  await page.goto("/lesson-sessions/lesson-session-ep006");
  await page.getByRole("button", { name: "开始录音" }).click();
  await expect(page.getByText("正在录音…说完后点击停止")).toBeVisible();
  await page.getByRole("button", { name: "停止并提交" }).click();
  await expect(page.getByText("请确认识别内容后再继续")).toBeVisible();
  await expect(page.getByRole("heading", { name: "把关键信息说给 Lin Muen" })).toBeVisible();
  await page.getByLabel("识别到的英文").fill("Your flight leaves from Gate 24.");
  await page.getByRole("button", { name: "修改后确认" }).click();
  await expect(page.getByRole("heading", { name: "和 Lin Muen 一起完成真实对话" })).toBeVisible();
  await expect(page.locator("vite-error-overlay, #webpack-dev-server-client-overlay, [data-nextjs-dialog]")).toHaveCount(0);
  expect(consoleErrors).toEqual([]);
});

test("streams and reconciles a bounded multi-turn role play without hiding the Lin Muen scene", async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") consoleErrors.push(message.text()); });
  await seedAuthenticatedLearner(page);
  let session = lessonSession("ROLE_PLAY");
  await mockBackend(page, () => session, (next) => { session = next; });

  await page.goto("/lesson-sessions/lesson-session-ep006");
  await expect(page.getByRole("img", { name: /Lin Muen stands full body near an airport boarding gate/ })).toBeVisible();
  await expect(page.getByRole("heading", { name: "和 Lin Muen 一起完成真实对话" })).toBeVisible();
  await expect(page.getByText("Traveler helping Lin Muen")).toBeVisible();
  await page.getByLabel("用英文回应").fill("Could you confirm Gate 24 and boarding at 3:20?");
  await page.getByRole("button", { name: "发送这一轮" }).click();
  await expect(page.getByText("Yes. Gate 24 is correct, and boarding begins at 3:20.")).toBeVisible();

  await page.reload();
  await expect(page.getByText("Could you confirm Gate 24 and boarding at 3:20?")).toBeVisible();
  await expect(page.getByText("Yes. Gate 24 is correct, and boarding begins at 3:20.")).toBeVisible();
  await expect(page.locator("vite-error-overlay, #webpack-dev-server-client-overlay, [data-nextjs-dialog]")).toHaveCount(0);
  expect(consoleErrors).toEqual([]);
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
  const rolePlayTurns: Array<Record<string, unknown>> = [];
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
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006/attempts", async (route) => {
    const request = await route.request().postDataJSON();
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    if (request.taskId === "gate-q1") {
      const next = lessonSession("COMPREHENSION", ["gate-q1"], ["gate-q2"]);
      update(next);
      await json(route, attemptReceipt("lat-q1", "gate-q1", "ANALYZED", next, true), 200);
      return;
    }
    if (request.taskId === "gate-q2") {
      const next = lessonSession("TRANSCRIPT_EXPRESSIONS");
      update(next);
      await json(route, attemptReceipt("lat-q2", "gate-q2", "ANALYZED", next, true), 200);
      return;
    }
    if (request.inputType === "AUDIO") {
      await json(route, {
        attemptId: "lat-audio", taskId: "gate-guided-1", inputType: "AUDIO", status: "RECEIVED",
        submittedAt: "2026-08-21T01:00:00Z", pollAfterMs: null,
        transcript: { text: "Your flight leaves from gate twenty four.", confidence: 0.42, confirmationRequired: true },
        stepProgress: current().attemptProgress, version: 2,
      });
      return;
    }
    const next = lessonSession("ROLE_PLAY", [], [], "lat-guided");
    update(next);
    await json(route, attemptReceipt("lat-guided", "gate-guided-1", "ANALYSIS_PENDING", next), 202);
  });
  await page.route("**/api/v1/audio/uploads", async (route) => {
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    expect(route.request().headers()["content-type"]).toContain("multipart/form-data");
    await json(route, { audioAssetId: "usr_audio_1", uploadStatus: "READY", mimeType: "audio/webm",
      durationMs: 100, contentHash: `sha256:${"0".repeat(64)}` }, 201);
  });
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006/role-play/turns", (route) =>
    json(route, { items: rolePlayTurns }));
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006/role-play/messages/stream", async (route) => {
    expect(route.request().headers()["idempotency-key"]).toBeTruthy();
    const request = await route.request().postDataJSON();
    rolePlayTurns.splice(0, rolePlayTurns.length, {
      turnId: request.conversationTurnId,
      attemptId: "att-role-1",
      taskId: request.taskId,
      learnerText: request.text,
      replyText: "Yes. Gate 24 is correct, and boarding begins at 3:20.",
      status: "COMPLETED",
      acceptedAt: "2026-08-21T01:00:00Z",
      completedAt: "2026-08-21T01:00:01Z",
      version: 2,
    });
    await route.fulfill({ status: 200, contentType: "text/event-stream", body: [
      `event: turn.accepted\ndata: {"attemptId":"att-role-1","turnId":"${request.conversationTurnId}","replayed":false}\n\n`,
      'event: reply.delta\ndata: {"sequence":1,"text":"Yes. Gate 24 is correct, and boarding begins at 3:20."}\n\n',
      `event: reply.completed\ndata: {"turnId":"${request.conversationTurnId}","messageId":"reply-1"}\n\n`,
      'event: analysis.pending\ndata: {"attemptId":"att-role-1"}\n\n',
    ].join("") });
  });
  await page.route("**/api/v1/lesson-sessions/lesson-session-ep006/attempts/lat-audio/transcript-confirmations", async (route) => {
    expect(await route.request().postDataJSON()).toEqual({
      decision: "CORRECT", correctedText: "Your flight leaves from Gate 24.",
    });
    const next = lessonSession("ROLE_PLAY", [], [], "lat-audio");
    update(next);
    await json(route, {
      attemptId: "lat-audio", taskId: "gate-guided-1", inputType: "AUDIO", status: "ANALYSIS_PENDING",
      submittedAt: "2026-08-21T01:00:00Z", pollAfterMs: 1000,
      transcript: { text: "Your flight leaves from Gate 24.", confidence: 0.42, confirmationRequired: false },
      stepProgress: next.attemptProgress, version: 3,
    });
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

function lessonSession(
  step: "SCENE_CONTEXT" | "FIRST_LISTEN" | "COMPREHENSION" | "TRANSCRIPT_EXPRESSIONS" | "GUIDED_SPEAKING" | "ROLE_PLAY",
  completedTaskIds: string[] = [],
  remainingTaskIds: string[] = step === "COMPREHENSION" ? ["gate-q1", "gate-q2"] : [],
  pendingAttemptId?: string,
) {
  const clientCompletable = ["SCENE_CONTEXT", "FIRST_LISTEN", "TRANSCRIPT_EXPRESSIONS"].includes(step);
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
    attemptProgress: {
      stepId: step,
      completedTaskIds,
      remainingTaskIds,
      nextStepEligible: step !== "COMPREHENSION" || remainingTaskIds.length === 0,
      pendingAttemptId,
    },
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
      questions: [
        { questionId: "gate-q1", prompt: "Which gate should Lin Muen use?", answer: "Gate 24" },
        { questionId: "gate-q2", prompt: "When does boarding begin?", answer: "At 3:20" },
      ],
      practice: [{
        taskId: "gate-guided-1", type: "guided_speaking",
        prompt: "Tell Lin Muen the new gate and boarding time in one clear response.",
        successCriteria: ["State Gate 24", "State 3:20"],
        scaffolding: ["Your flight leaves from...", "Boarding begins at..."],
      }],
      rolePlay: {
        taskId: "gate-roleplay-1",
        goal: "Confirm the changed gate and boarding time with the airport agent.",
        userRole: "Traveler helping Lin Muen",
        aiRole: "Airport agent",
        successCriteria: ["Confirm Gate 24", "Confirm boarding at 3:20"],
        openingLine: "Good afternoon. How can I help you and Lin Muen?",
      },
    } },
    assets: [], publishedAt: "2026-08-20T00:00:00Z",
  };
}

function attemptReceipt(
  attemptId: string,
  taskId: string,
  status: "ANALYZED" | "ANALYSIS_PENDING",
  session: ReturnType<typeof lessonSession>,
  correct = false,
) {
  return {
    attemptId, taskId, inputType: "TEXT", status, submittedAt: "2026-08-21T01:00:00Z",
    pollAfterMs: status === "ANALYSIS_PENDING" ? 1000 : null,
    objectiveResult: status === "ANALYZED" ? { correct, expectedAnswer: taskId === "gate-q1" ? "Gate 24" : "At 3:20", explanation: "Answer confirmed." } : null,
    stepProgress: session.attemptProgress,
    version: 1,
  };
}
