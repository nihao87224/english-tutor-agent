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

  it("keeps X-User-Key fallback only when no access token is configured", async () => {
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
      jsonResponse({ planId: "p1", date: "2026-08-10", totalMinutes: 10, tasks: [], reasons: [] }),
    );
    const client = createApiClient({ baseUrl: "http://api.test", userKey: "legacy-user", fetchFn });

    await client.getTodayPlan();

    const [, init] = lastFetchCall(fetchFn);
    const headers = init.headers as Headers;
    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("X-User-Key")).toBe("legacy-user");
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
