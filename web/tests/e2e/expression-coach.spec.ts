import { expect, test, type Page } from "@playwright/test";

test("registers, practices, consumes quota, logs out and logs back in", async ({ page }) => {
  await mockBackend(page);

  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Sign in to English Tutor" })).toBeVisible();
  await page.getByRole("button", { name: "Sign up", exact: true }).first().click();
  await page.getByLabel("Email").fill("learner@example.com");
  await page.getByLabel("Password").fill("learner-password");
  await page.getByRole("button", { name: "Sign up" }).last().click();

  await expect(page.getByRole("heading", { name: "Set up your expression coach." })).toBeVisible();
  await page.getByRole("button", { name: "Enter today's coach" }).click();

  await page.getByRole("button", { name: "AI coach" }).click();
  await expect(page.getByRole("heading", { name: "Improve one sentence" })).toBeVisible();
  await expect(page.getByText("50 left")).toBeVisible();
  await page.getByRole("button", { name: "Start practice" }).click();

  await expect(page.getByText("Write one sentence or mixed-language idea.")).toBeVisible();
  await page.getByRole("textbox").fill("I very like this movie");
  await page.getByRole("button", { name: "Send" }).click();

  await expect(page.getByText("Use really before verbs like like.")).toBeVisible();
  await expect(page.getByRole("listitem").filter({ hasText: "I really like this movie." })).toBeVisible();
  await expect(page.getByLabel("Correction panel").getByRole("button", { name: "Try Again" })).toBeVisible();

  await page.getByLabel("Correction panel").getByRole("button", { name: "Try Again" }).click();
  await expect(page.getByRole("textbox")).toHaveValue("I really like this movie.");
  await page.getByLabel("Conversation").getByRole("button", { name: "Try Again" }).click();

  await expect(page.locator(".chat-message.retry")).toBeVisible();
  await expect(page.getByLabel("Conversation").getByText("Nice improvement.")).toBeVisible();

  await page.getByRole("button", { name: "Complete practice" }).click();

  await expect(page.getByRole("heading", { name: "Practice completed." })).toBeVisible();
  await expect(page.getByText("really like")).toBeVisible();
  await expect(page.getByText("Use natural adverbs before verbs.")).toBeVisible();

  await page.getByRole("button", { name: "Back to today's coach" }).click();
  await expect(page.getByText("48 left")).toBeVisible();
  await page.getByRole("button", { name: "History" }).click();
  await expect(page.getByRole("heading", { name: "Practice history" })).toBeVisible();
  await expect(page.getByText("You replaced direct translation with a natural adverb pattern.")).toBeVisible();
  await page.getByRole("button", { name: "Log out" }).click();

  await expect(page.getByRole("heading", { name: "Sign in to English Tutor" })).toBeVisible();
  await page.getByLabel("Email").fill("learner@example.com");
  await page.getByLabel("Password").fill("learner-password");
  await page.getByRole("button", { name: "Log in" }).last().click();

  await page.getByRole("button", { name: "AI coach" }).click();
  await expect(page.getByRole("heading", { name: "Improve one sentence" })).toBeVisible();
  await expect(page.getByText("48 left")).toBeVisible();
  await page.getByRole("button", { name: "History" }).click();
  await expect(page.getByText("Use natural adverbs before verbs.")).toBeVisible();
});

async function mockBackend(page: Page) {
  let streamCallCount = 0;
  let onboardingCompleted = false;
  let quotaUsed = 0;

  const user = {
    userKey: "learner-user",
    email: "learner@example.com",
    status: "ACTIVE",
    roles: ["USER"],
    locale: "en",
    timezone: "UTC",
  };

  await page.route("http://localhost:8080/api/v1/auth/refresh", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/problem+json",
      body: JSON.stringify({ type: "about:blank", title: "Unauthorized", status: 401 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/auth/register", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: { "set-cookie": "ETA_REFRESH_TOKEN=refresh; Path=/api/v1/auth; HttpOnly; SameSite=Lax" },
      body: JSON.stringify({ user, accessToken: "access-token", expiresIn: 3600 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: { "set-cookie": "ETA_REFRESH_TOKEN=refresh; Path=/api/v1/auth; HttpOnly; SameSite=Lax" },
      body: JSON.stringify({ user, accessToken: "access-token-2", expiresIn: 3600 }),
    });
  });

  await page.route("http://localhost:8080/api/v1/auth/logout", async (route) => {
    await route.fulfill({ status: 200 });
  });

  await page.route("http://localhost:8080/api/v1/onboarding/progress", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ step: onboardingCompleted ? "COMPLETE" : "GOAL", completed: onboardingCompleted }),
    });
  });

  await page.route("http://localhost:8080/api/v1/users/me/progress", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        nextStep: onboardingCompleted ? "READY_FOR_PLAN" : "ONBOARDING_REQUIRED",
        onboardingStep: onboardingCompleted ? "COMPLETE" : "GOAL",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/me/quota", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        quotaDate: "2026-08-10",
        dailyLimit: 50,
        used: quotaUsed,
        bonus: 0,
        remaining: 50 - quotaUsed,
        unlimited: false,
        resetAt: "2026-08-11T00:00:00Z",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/profile/primary-goal", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ primaryGoal: "WORKPLACE", dailyMinutes: 10, correctionStyle: "STANDARD", onboardingCompleted: false }),
    });
  });

  await page.route("http://localhost:8080/api/v1/profile/preferences", async (route) => {
    onboardingCompleted = true;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ primaryGoal: "WORKPLACE", dailyMinutes: 10, correctionStyle: "STANDARD", onboardingCompleted: true }),
    });
  });

  await page.route("http://localhost:8080/api/v1/plans/today", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        planId: "plan-1",
        date: "2026-08-10",
        totalMinutes: 10,
        reasons: ["Direct translation is your next expression bottleneck."],
        tasks: [
          {
            taskId: "task-1",
            type: "CONVERSATION",
            title: "Improve one sentence",
            durationMinutes: 10,
            skillFocus: ["expression"],
            difficulty: "EASY",
            reason: "Turn a direct sentence into natural English.",
          },
        ],
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/prescriptions/today?**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        prescriptionId: "prx-expression",
        version: 1,
        learningDate: "2026-08-10",
        timezone: "UTC",
        status: "ACTIVE",
        priorityGoal: { code: "EXPRESSION", label: "Build natural expression" },
        rationale: "Direct translation is your next expression bottleneck.",
        reasonCodes: ["ERROR_MATCH"],
        estimatedMinutes: 10,
        experience: { seasonId: "S01", episodeId: "EP001", sceneId: "daily_expression", title: "Daily Expression" },
        blocks: [{
          blockId: "block-expression",
          sequence: 1,
          type: "OUTPUT",
          title: "Improve one sentence",
          skillUnitVariantId: "expression.natural.a2",
          resource: { resourceId: "expression.resource", resourceVersion: "1.0.0" },
          episodeMappingId: "mapping-expression",
          difficulty: "A2",
          scaffolding: "HIGH",
          trainingType: "GUIDED_SPEAKING",
          estimatedMinutes: 10,
          expectedEvidence: ["natural_expression"],
          recommendationFactors: { errorMatch: 1 },
          taskHero: {
            assetId: "expression-hero",
            url: null,
            aspectRatio: "16:9",
            focalPoint: { x: 0.5, y: 0.5 },
            altText: "Lin Muen practices a natural daily expression with the learner.",
          },
          status: "READY",
        }],
        generatedAt: "2026-08-10T00:00:00Z",
        expiresAt: "2026-08-11T00:00:00Z",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/training-sessions", async (route) => {
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({
        sessionId: "session-1",
        planId: "plan-1",
        type: "DAILY",
        mode: "TEXT",
        status: "IN_PROGRESS",
        currentTaskId: "task-1",
        startedAt: "2026-08-10T00:00:00Z",
        effectiveSeconds: 0,
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/training-sessions/session-1/current-task", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        taskId: "task-1",
        type: "CONVERSATION",
        title: "Improve one sentence",
        durationMinutes: 10,
        skillFocus: ["expression"],
        difficulty: "EASY",
        reason: "Turn a direct sentence into natural English.",
        status: "READY",
      }),
    });
  });

  await page.route("http://localhost:8080/api/v1/conversations/session-1/messages/stream", async (route) => {
    streamCallCount += 1;
    quotaUsed += 1;
    const firstCorrection = {
      hasError: true,
      corrections: [
        {
          original: "very like",
          corrected: "really like",
          errorType: "word_choice",
          severity: "MEDIUM",
          explanationZh: "Use really before verbs like like.",
          shouldInterrupt: false,
          memoryWorthy: true,
          naturalSuggestions: [{ sentence: "I really like this movie.", style: "NEUTRAL" }],
        },
      ],
      overallFeedback: "Good communication. Fix the adverb before the verb.",
      promptVersion: "correction-analyzer-v1",
      schemaVersion: "correction-result-v1",
      traceId: "correction-1",
      providerId: "openai",
      modelId: "test-chat-model",
    };
    const retryCorrection = {
      ...firstCorrection,
      hasError: false,
      corrections: [],
      overallFeedback: "Nice improvement.",
      traceId: "correction-2",
    };

    await route.fulfill({
      status: 200,
      contentType: "text/event-stream",
      body: [
        'event: status\ndata: {"stage":"THINKING","message":"Thinking..."}\n\n',
        `event: text_delta\ndata: {"delta":"${streamCallCount === 1 ? "Try: I really like this movie." : "Nice improvement."}"}\n\n`,
        `event: correction_ready\ndata: ${JSON.stringify(streamCallCount === 1 ? firstCorrection : retryCorrection)}\n\n`,
        'event: done\ndata: {"traceId":"conversation-1","providerId":"openai","modelId":"test-chat-model"}\n\n',
      ].join(""),
    });
  });

  await page.route("http://localhost:8080/api/v1/training-sessions/session-1/complete", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        session: {
          sessionId: "session-1",
          planId: "plan-1",
          type: "DAILY",
          mode: "TEXT",
          status: "COMPLETED",
          currentTaskId: "task-1",
          startedAt: "2026-08-10T00:00:00Z",
          completedAt: "2026-08-10T00:05:00Z",
          effectiveSeconds: 300,
        },
        dailySummary: {
          sessionId: "session-1",
          completedTaskCount: 1,
          evidenceCount: 2,
          practicedSkills: ["expression"],
          highlights: ["You replaced direct translation with a natural adverb pattern."],
          memorableItems: ["really like"],
          nextFocus: ["Use natural adverbs before verbs."],
          generatedAt: "2026-08-10T00:05:00Z",
        },
      }),
    });
  });
}
