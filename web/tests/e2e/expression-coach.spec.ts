import { expect, test, type Page } from "@playwright/test";

test("completes the Web expression coach path", async ({ page }) => {
  await mockBackend(page);

  await page.goto("/");

  await expect(page.getByRole("heading", { name: "先把表达教练跑起来。" })).toBeVisible();
  await page.getByRole("button", { name: "Enter today's coach" }).click();

  await expect(page.getByRole("heading", { name: "Improve one sentence" })).toBeVisible();
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
});

async function mockBackend(page: Page) {
  let streamCallCount = 0;

  await page.route("http://localhost:8080/api/v1/profile/primary-goal", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ primaryGoal: "WORKPLACE", dailyMinutes: 10, correctionStyle: "STANDARD", onboardingCompleted: false }),
    });
  });

  await page.route("http://localhost:8080/api/v1/profile/preferences", async (route) => {
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
