import { describe, expect, it, vi } from "vitest";
import { ApiError, createApiClient } from "./client";
import type { SseEvent } from "./sse";

describe("createApiClient", () => {
  it("sends bearer token and idempotency headers for mutations", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ primaryGoal: "WORKPLACE", dailyMinutes: 10, correctionStyle: "STANDARD" }),
    );
    const client = createApiClient({
      baseUrl: "http://api.test/",
      accessToken: "access-token",
      fetchFn,
      idempotencyKeyFactory: () => "fixed-key",
    });

    await client.putPrimaryGoal("WORKPLACE");

    const [url, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(String(url)).toBe("http://api.test/api/v1/profile/primary-goal");
    expect(init.method).toBe("PUT");
    expect(init.credentials).toBe("include");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.get("X-User-Key")).toBeNull();
    expect(headers.get("Idempotency-Key")).toBe("fixed-key");
    expect(JSON.parse(init.body as string)).toEqual({ goal: "WORKPLACE" });
  });

  it("does not attach idempotency headers to GET requests", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ planId: "p1", date: "2026-08-10", totalMinutes: 10, tasks: [], reasons: [] }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "access-token", fetchFn });

    await client.getTodayPlan();

    const [, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(init.method).toBe("GET");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.get("Idempotency-Key")).toBeNull();
  });

  it("loads today's prescription with an encoded learner timezone", async () => {
    const fetchFn = vi.fn(async () => jsonResponse({ prescriptionId: "prx-1", blocks: [] }));
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "access-token", fetchFn });

    await client.getTodayPrescription({ timezone: "Asia/Shanghai" });

    const [url, init] = lastFetchCall(fetchFn);
    expect(String(url)).toBe("http://api.test/api/v1/prescriptions/today?timezone=Asia%2FShanghai");
    expect(init.method).toBe("GET");
    expect((init.headers as Headers).get("Idempotency-Key")).toBeNull();
  });

  it("uses the same explicit idempotency key when a regeneration response is replayed", async () => {
    const response = { prescriptionId: "prx-2", version: 2, blocks: [] };
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => jsonResponse(response));
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "access-token", fetchFn });
    const request = { currentPrescriptionId: "prx-1", currentVersion: 1, reason: "TIME_INSUFFICIENT" as const, availableMinutes: 10 };

    const first = await client.regenerateTodayPrescription(request, { idempotencyKey: "regen-replay-key" });
    const replayed = await client.regenerateTodayPrescription(request, { idempotencyKey: "regen-replay-key" });

    expect(first).toEqual(replayed);
    expect(fetchFn).toHaveBeenCalledTimes(2);
    for (const call of fetchFn.mock.calls) {
      const init = call[1] as RequestInit;
      expect((init.headers as Headers).get("Idempotency-Key")).toBe("regen-replay-key");
      expect(JSON.parse(init.body as string)).toEqual(request);
    }
  });

  it("uses canonical lesson-session, locked resource and media routes", async () => {
    const fetchFn = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/media-access")) return jsonResponse({ assetId: "hero-1", url: "https://cdn.test/hero.webp" });
      if (url.includes("/learning-resources/")) return jsonResponse({ resourceId: "resource-1", resourceVersion: "1.0.0" });
      return jsonResponse({ sessionId: "lesson-1", currentStep: "FIRST_LISTEN" });
    });
    const client = createApiClient({
      baseUrl: "http://api.test",
      accessToken: "access-token",
      fetchFn,
      idempotencyKeyFactory: () => "lesson-idem-key",
    });

    await client.startLessonSession({ prescriptionId: "prx-1", prescriptionVersion: 2, blockId: "block-1", inputMode: "VOICE_OR_TEXT" });
    await client.getLessonSession("lesson/1");
    await client.pauseLessonSession("lesson/1");
    await client.resumeLessonSession("lesson/1");
    await client.completeLessonStep("lesson/1", "SCENE_CONTEXT");
    await client.getLearningResourceVersion("resource/1", "1.0.0+b1");
    await client.createLearningResourceMediaAccess("resource/1", { assetId: "hero-1", purpose: "DISPLAY" });

    expect(fetchFn.mock.calls.map((call) => String(call[0]))).toEqual([
      "http://api.test/api/v1/lesson-sessions",
      "http://api.test/api/v1/lesson-sessions/lesson%2F1",
      "http://api.test/api/v1/lesson-sessions/lesson%2F1/pause",
      "http://api.test/api/v1/lesson-sessions/lesson%2F1/resume",
      "http://api.test/api/v1/lesson-sessions/lesson%2F1/steps/SCENE_CONTEXT/completions",
      "http://api.test/api/v1/learning-resources/resource%2F1/versions/1.0.0%2Bb1",
      "http://api.test/api/v1/learning-resources/resource%2F1/media-access",
    ]);
    const start = fetchFn.mock.calls[0][1] as RequestInit;
    const completion = fetchFn.mock.calls[4][1] as RequestInit;
    const media = fetchFn.mock.calls[6][1] as RequestInit;
    expect((start.headers as Headers).get("Idempotency-Key")).toBe("lesson-idem-key");
    expect((completion.headers as Headers).get("Idempotency-Key")).toBe("lesson-idem-key");
    expect((media.headers as Headers).get("Idempotency-Key")).toBe("lesson-idem-key");
    expect(JSON.parse(media.body as string)).toEqual({ assetId: "hero-1", purpose: "DISPLAY" });
  });

  it("uploads audio as multipart and confirms a low-confidence transcript", async () => {
    const fetchFn = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => String(input).endsWith("/audio/uploads")
      ? jsonResponse({ audioAssetId: "usr_audio_1", uploadStatus: "READY", mimeType: "audio/webm", durationMs: 900,
        contentHash: `sha256:${"0".repeat(64)}` })
      : jsonResponse({ attemptId: "lat-1", taskId: "guided-1", inputType: "AUDIO", status: "ANALYSIS_PENDING" }));
    const client = createApiClient({
      baseUrl: "http://api.test",
      accessToken: "access-token",
      fetchFn,
      idempotencyKeyFactory: () => "audio-key",
    });

    await client.uploadAudio({ file: new Blob(["voice"], { type: "audio/webm" }), durationMs: 900 });
    await client.confirmLessonAttemptTranscript("lesson/1", "lat/1", { decision: "CORRECT", correctedText: "Gate 24" });

    const upload = fetchFn.mock.calls[0][1] as RequestInit;
    expect(upload.body).toBeInstanceOf(FormData);
    expect((upload.headers as Headers).get("Content-Type")).toBeNull();
    expect((upload.headers as Headers).get("Idempotency-Key")).toBe("audio-key");
    expect(String(fetchFn.mock.calls[1][0])).toContain("lesson%2F1/attempts/lat%2F1/transcript-confirmations");
    expect(JSON.parse(fetchFn.mock.calls[1][1]?.body as string)).toEqual({ decision: "CORRECT", correctedText: "Gate 24" });
  });

  it("preserves prescription fallback details from API errors", async () => {
    const fetchFn = vi.fn(async () => jsonResponse({
      type: "about:blank",
      title: "Conflict",
      status: 409,
      detail: "no eligible prescription candidate",
      code: "PRESCRIPTION_NO_CANDIDATE",
      fallbackAvailable: true,
    }, 409));
    const client = createApiClient({ baseUrl: "http://api.test", fetchFn });

    await expect(client.getTodayPrescription()).rejects.toMatchObject({
      status: 409,
      problem: { code: "PRESCRIPTION_NO_CANDIDATE", fallbackAvailable: true },
    });
  });

  it("throws ApiError with problem details for failed responses", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ type: "about:blank", title: "Bad Request", status: 400, detail: "invalid plan" }, 400),
    );
    const client = createApiClient({ baseUrl: "http://api.test", fetchFn });

    await expect(client.getTodayPlan()).rejects.toMatchObject({
      name: "ApiError",
      status: 400,
      message: "invalid plan",
    });
  });

  it("streams POST SSE conversation events", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      new Response(streamFromChunks(['event: text_delta\ndata: {"delta":"Hi"}\n\n']), {
        status: 200,
        headers: { "Content-Type": "text/event-stream" },
      }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "access-token", fetchFn, idempotencyKeyFactory: () => "sse-key" });
    const events: SseEvent[] = [];

    await client.streamConversationMessage("session-1", { messageType: "TEXT", text: "I very like this movie" }, (event) =>
      events.push(event),
    );

    const [url, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(String(url)).toBe("http://api.test/api/v1/conversations/session-1/messages/stream");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("include");
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.get("Idempotency-Key")).toBe("sse-key");
    expect(events).toEqual([{ event: "text_delta", data: { delta: "Hi" } }]);
  });

  it("streams protected lesson role-play events and reconciles persisted turns", async () => {
    const fetchFn = vi.fn()
      .mockResolvedValueOnce(new Response(streamFromChunks([
        'event: turn.accepted\ndata: {"attemptId":"att-1","turnId":"turn-1","replayed":false}\n\n',
        'event: reply.delta\ndata: {"sequence":1,"text":"Gate 24."}\n\n',
      ]), { status: 200, headers: { "Content-Type": "text/event-stream" } }))
      .mockResolvedValueOnce(jsonResponse({ items: [{ turnId: "turn-1", status: "COMPLETED" }] }));
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "token", fetchFn });
    const events: SseEvent[] = [];

    await client.streamRolePlayMessage("lesson/1", {
      taskId: "gate-role", text: "Gate 24?", conversationTurnId: "turn-1",
    }, (event) => events.push(event), { idempotencyKey: "turn-1" });
    const turns = await client.listRolePlayTurns("lesson/1");

    expect(String(fetchFn.mock.calls[0][0])).toContain("lesson-sessions/lesson%2F1/role-play/messages/stream");
    expect((fetchFn.mock.calls[0][1]?.headers as Headers).get("Idempotency-Key")).toBe("turn-1");
    expect(events.map((event) => event.event)).toEqual(["turn.accepted", "reply.delta"]);
    expect(turns.items[0].status).toBe("COMPLETED");
    expect(String(fetchFn.mock.calls[1][0])).toContain("lesson-sessions/lesson%2F1/role-play/turns");
  });

  it("does not send legacy identity headers when no access token is configured", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ planId: "p1", date: "2026-08-10", totalMinutes: 10, tasks: [], reasons: [] }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", fetchFn });

    await client.getTodayPlan();

    const [, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("X-User-Key")).toBeNull();
  });

  it("refreshes once and retries after an unauthorized response", async () => {
    let token = "expired";
    const fetchFn = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ type: "about:blank", title: "Unauthorized", status: 401 }, 401))
      .mockResolvedValueOnce(jsonResponse({ planId: "p1", date: "2026-08-10", totalMinutes: 10, tasks: [], reasons: [] }));
    const client = createApiClient({
      baseUrl: "http://api.test",
      accessTokenProvider: () => token,
      fetchFn,
      onUnauthorized: async () => {
        token = "fresh";
        return true;
      },
    });

    await client.getTodayPlan();

    expect(fetchFn).toHaveBeenCalledTimes(2);
    const [, retryInit] = lastFetchCall(fetchFn);
    expect((retryInit.headers as Headers).get("Authorization")).toBe("Bearer fresh");
  });

  it("calls admin search users with filters and bearer auth", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ items: [], page: 0, size: 20, total: 0 }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "admin-token", fetchFn });

    await client.searchAdminUsers({ q: "learner@example.com", status: "ACTIVE", role: "USER", page: 1, size: 10 });

    const [url, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(String(url)).toBe("http://api.test/api/v1/admin/users?q=learner%40example.com&status=ACTIVE&role=USER&page=1&size=10");
    expect(init.method).toBe("GET");
    expect(headers.get("Authorization")).toBe("Bearer admin-token");
    expect(headers.get("Idempotency-Key")).toBeNull();
  });

  it("sends idempotency keys for admin mutations", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({
        userKey: "learner-user",
        unlimited: false,
        quotaDate: "2026-08-10",
        dailyLimit: 30,
        used: 0,
        reserved: 0,
        bonus: 0,
        remaining: 30,
      }),
    );
    const client = createApiClient({
      baseUrl: "http://api.test",
      accessToken: "admin-token",
      fetchFn,
      idempotencyKeyFactory: () => "admin-key",
    });

    await client.updateAdminQuotaPolicy("learner/user", { dailyLimitOverride: 30, unlimited: false });

    const [url, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(String(url)).toBe("http://api.test/api/v1/admin/users/learner%2Fuser/quota-policy");
    expect(init.method).toBe("PUT");
    expect(headers.get("Idempotency-Key")).toBe("admin-key");
    expect(JSON.parse(init.body as string)).toEqual({ dailyLimitOverride: 30, unlimited: false });
  });

  it("replaces provider secrets without exposing them in the URL", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({
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
        apiKeyConfigured: true,
        apiKeyMaskedHint: "sk-...abcd",
      }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", accessToken: "admin-token", fetchFn });

    await client.replaceAiProviderSecret("openai", { apiKey: "sk-secret" });

    const [url, init] = lastFetchCall(fetchFn);
    expect(String(url)).toBe("http://api.test/api/v1/admin/ai-providers/openai/secret");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual({ apiKey: "sk-secret" });
  });

  it("tests a saved provider connection with an administrator request", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ success: false, latencyMs: 12, error: "INVALID_API_KEY" }),
    );
    const client = createApiClient({
      baseUrl: "http://api.test",
      accessToken: "admin-token",
      fetchFn,
      idempotencyKeyFactory: () => "connection-test-key",
    });

    await expect(client.testAiProviderConnection("openai")).resolves.toEqual({
      success: false,
      latencyMs: 12,
      error: "INVALID_API_KEY",
    });

    const [url, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(String(url)).toBe("http://api.test/api/v1/admin/ai-providers/openai/test");
    expect(init.method).toBe("POST");
    expect(headers.get("Idempotency-Key")).toBe("connection-test-key");
  });

  it("accepts empty logout responses", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response("", { status: 200 }));
    const client = createApiClient({ baseUrl: "http://api.test", fetchFn });

    await expect(client.logout()).resolves.toBeUndefined();
  });
});

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function lastFetchCall(fetchFn: ReturnType<typeof vi.fn>): [RequestInfo | URL, RequestInit] {
  const call = fetchFn.mock.calls.at(-1);
  if (!call || !call[1]) {
    throw new Error("expected fetch to be called with RequestInit");
  }
  return [call[0] as RequestInfo | URL, call[1] as RequestInit];
}

function streamFromChunks(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
      controller.close();
    },
  });
}
