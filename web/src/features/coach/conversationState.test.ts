import { describe, expect, it } from "vitest";
import {
  EMPTY_CONVERSATION_STATE,
  applySseEvent,
  failStream,
  startUserMessage,
  type ConversationState,
} from "./conversationState";

describe("conversationState", () => {
  it("adds user and assistant messages when a stream starts", () => {
    const state = startUserMessage(EMPTY_CONVERSATION_STATE, "I very like this movie", idSequence("a1", "u1"));

    expect(state.streamStatus).toBe("streaming");
    expect(state.messages).toEqual([
      { id: "u1", role: "user", text: "I very like this movie", kind: "initial" },
      { id: "a1", role: "assistant", text: "" },
    ]);
  });

  it("marks retry attempts", () => {
    const state = startUserMessage(EMPTY_CONVERSATION_STATE, "I really like this movie", idSequence("a1", "u1"), "retry");

    expect(state.messages[0]).toMatchObject({ role: "user", kind: "retry" });
  });

  it("appends text deltas to the active assistant message", () => {
    const initial = startUserMessage(EMPTY_CONVERSATION_STATE, "hello", idSequence("a1", "u1"));

    const next = applySseEvent(applySseEvent(initial, textDelta("This is ")), textDelta("better."));

    expect(next.messages[1]?.text).toBe("This is better.");
  });

  it("stores status and correction events", () => {
    const initial = startUserMessage(EMPTY_CONVERSATION_STATE, "hello", idSequence("a1", "u1"));
    const withStatus = applySseEvent(initial, { event: "status", data: { stage: "THINKING", message: "Thinking..." } });
    const withCorrection = applySseEvent(withStatus, {
      event: "correction_ready",
      data: {
        hasError: true,
        corrections: [],
        overallFeedback: "Good start.",
        promptVersion: "p1",
        schemaVersion: "s1",
        traceId: "t1",
        providerId: "fake",
        modelId: "fake",
      },
    });

    expect(withCorrection.statusMessage).toBe("Thinking...");
    expect(withCorrection.latestCorrection?.traceId).toBe("t1");
  });

  it("keeps partial text when a stream fails", () => {
    const streaming = applySseEvent(startUserMessage(EMPTY_CONVERSATION_STATE, "hello", idSequence("a1", "u1")), textDelta("Partial"));

    const failed = failStream(streaming, "network failed");

    expect(failed.streamStatus).toBe("error");
    expect(failed.errorMessage).toBe("network failed");
    expect(failed.messages[1]?.text).toBe("Partial");
  });
});

function textDelta(delta: string) {
  return { event: "text_delta" as const, data: { delta } };
}

function idSequence(...ids: string[]): () => string {
  let index = 0;
  return () => ids[index++] ?? `id-${index}`;
}
