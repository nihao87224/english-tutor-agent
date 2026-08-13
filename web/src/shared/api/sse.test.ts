import { describe, expect, it } from "vitest";
import { parseSseStream, type SseEvent } from "./sse";

describe("parseSseStream", () => {
  it("parses known events split across chunks", async () => {
    const events: SseEvent[] = [];
    const stream = streamFromChunks([
      'id: 1\nevent: status\ndata: {"stage":"THINKING","message":"Working"}\n\nid: 2\nevent: text_delta\ndata:',
      ' {"delta":"This is "}\n\nid: 3\nevent: done\ndata: {"traceId":"t","providerId":"fake","modelId":"fake-chat"}\n\n',
    ]);

    await parseSseStream(stream, (event) => events.push(event));

    expect(events).toHaveLength(3);
    expect(events[0]).toMatchObject({ id: "1", event: "status", data: { stage: "THINKING" } });
    expect(events[1]).toMatchObject({ id: "2", event: "text_delta", data: { delta: "This is " } });
    expect(events[2]).toMatchObject({ id: "3", event: "done", data: { traceId: "t" } });
  });

  it("keeps unknown or invalid events inspectable instead of throwing", async () => {
    const events: SseEvent[] = [];
    const stream = streamFromChunks(["event: custom\ndata: not-json\n\n"]);

    await parseSseStream(stream, (event) => events.push(event));

    expect(events).toEqual([
      {
        event: "unknown",
        originalEvent: "custom",
        data: { raw: "not-json", parseError: true },
      },
    ]);
  });

  it("joins multiline data fields before parsing JSON", async () => {
    const events: SseEvent[] = [];
    const stream = streamFromChunks(['event: text_delta\ndata: {"delta":"hello\\n"\ndata: }\n\n']);

    await parseSseStream(stream, (event) => events.push(event));

    expect(events[0]).toMatchObject({ event: "text_delta", data: { delta: "hello\n" } });
  });
});

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
