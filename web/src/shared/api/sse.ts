import type {
  CorrectionReadyEventData,
  DoneEventData,
  StatusEventData,
  TextDeltaEventData,
  RolePlayAnalysisPendingEventData,
  RolePlayReplyCompletedEventData,
  RolePlayReplyDeltaEventData,
  RolePlayStreamErrorEventData,
  RolePlayTurnAcceptedEventData,
} from "./types";

export type SseEvent =
  | { id?: string; event: "status"; data: StatusEventData }
  | { id?: string; event: "text_delta"; data: TextDeltaEventData }
  | { id?: string; event: "correction_ready"; data: CorrectionReadyEventData }
  | { id?: string; event: "done"; data: DoneEventData }
  | { id?: string; event: "turn.accepted"; data: RolePlayTurnAcceptedEventData }
  | { id?: string; event: "reply.delta"; data: RolePlayReplyDeltaEventData }
  | { id?: string; event: "reply.completed"; data: RolePlayReplyCompletedEventData }
  | { id?: string; event: "analysis.pending"; data: RolePlayAnalysisPendingEventData }
  | { id?: string; event: "stream.error"; data: RolePlayStreamErrorEventData }
  | { id?: string; event: "unknown"; originalEvent: string; data: unknown };

export type SseEventHandler = (event: SseEvent) => void;

interface RawSseEvent {
  id?: string;
  event: string;
  data: string;
}

export async function parseSseStream(stream: ReadableStream<Uint8Array>, onEvent: SseEventHandler): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    buffer = drainBuffer(buffer, onEvent);
  }

  buffer += decoder.decode();
  drainBuffer(`${buffer}\n\n`, onEvent);
}

function drainBuffer(buffer: string, onEvent: SseEventHandler): string {
  const normalized = buffer.replace(/\r\n/g, "\n");
  const frames = normalized.split("\n\n");
  const remainder = frames.pop() ?? "";

  for (const frame of frames) {
    const rawEvent = parseFrame(frame);
    if (rawEvent) {
      onEvent(toTypedEvent(rawEvent));
    }
  }

  return remainder;
}

function parseFrame(frame: string): RawSseEvent | null {
  const data: string[] = [];
  let event = "message";
  let id: string | undefined;

  for (const line of frame.split("\n")) {
    if (!line || line.startsWith(":")) {
      continue;
    }

    const separator = line.indexOf(":");
    const field = separator === -1 ? line : line.slice(0, separator);
    const rawValue = separator === -1 ? "" : line.slice(separator + 1);
    const value = rawValue.startsWith(" ") ? rawValue.slice(1) : rawValue;

    if (field === "event") {
      event = value;
    } else if (field === "data") {
      data.push(value);
    } else if (field === "id") {
      id = value;
    }
  }

  if (data.length === 0) {
    return null;
  }

  return { id, event, data: data.join("\n") };
}

function toTypedEvent(rawEvent: RawSseEvent): SseEvent {
  const data = parseJson(rawEvent.data);
  if (rawEvent.event === "status") {
    return { id: rawEvent.id, event: "status", data: data as StatusEventData };
  }
  if (rawEvent.event === "text_delta") {
    return { id: rawEvent.id, event: "text_delta", data: data as TextDeltaEventData };
  }
  if (rawEvent.event === "correction_ready") {
    return { id: rawEvent.id, event: "correction_ready", data: data as CorrectionReadyEventData };
  }
  if (rawEvent.event === "done") {
    return { id: rawEvent.id, event: "done", data: data as DoneEventData };
  }
  if (rawEvent.event === "turn.accepted") return { id: rawEvent.id, event: "turn.accepted", data: data as RolePlayTurnAcceptedEventData };
  if (rawEvent.event === "reply.delta") return { id: rawEvent.id, event: "reply.delta", data: data as RolePlayReplyDeltaEventData };
  if (rawEvent.event === "reply.completed") return { id: rawEvent.id, event: "reply.completed", data: data as RolePlayReplyCompletedEventData };
  if (rawEvent.event === "analysis.pending") return { id: rawEvent.id, event: "analysis.pending", data: data as RolePlayAnalysisPendingEventData };
  if (rawEvent.event === "stream.error") return { id: rawEvent.id, event: "stream.error", data: data as RolePlayStreamErrorEventData };
  return { id: rawEvent.id, event: "unknown", originalEvent: rawEvent.event, data };
}

function parseJson(data: string): unknown {
  try {
    return JSON.parse(data);
  } catch {
    return { raw: data, parseError: true };
  }
}
