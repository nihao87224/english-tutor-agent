import type { CorrectionReadyEventData } from "../../shared/api";
import type { SseEvent } from "../../shared/api";

export interface ConversationMessage {
  id: string;
  role: "user" | "assistant";
  text: string;
  kind?: "initial" | "retry";
}

export interface ConversationState {
  messages: ConversationMessage[];
  streamStatus: "idle" | "streaming" | "error";
  statusMessage: string;
  activeAssistantMessageId?: string;
  latestCorrection?: CorrectionReadyEventData;
  errorMessage?: string;
}

export const EMPTY_CONVERSATION_STATE: ConversationState = {
  messages: [],
  streamStatus: "idle",
  statusMessage: "",
};

export function startUserMessage(
  state: ConversationState,
  userText: string,
  idFactory: () => string,
  kind: ConversationMessage["kind"] = "initial",
): ConversationState {
  const assistantMessageId = idFactory();
  return {
    ...state,
    streamStatus: "streaming",
    statusMessage: "Sending expression...",
    errorMessage: undefined,
    activeAssistantMessageId: assistantMessageId,
    messages: [
      ...state.messages,
      { id: idFactory(), role: "user", text: userText, kind },
      { id: assistantMessageId, role: "assistant", text: "" },
    ],
  };
}

export function applySseEvent(state: ConversationState, event: SseEvent): ConversationState {
  if (event.event === "status") {
    return { ...state, statusMessage: event.data.message };
  }

  if (event.event === "text_delta") {
    return {
      ...state,
      messages: state.messages.map((message) =>
        message.id === state.activeAssistantMessageId ? { ...message, text: `${message.text}${event.data.delta}` } : message,
      ),
    };
  }

  if (event.event === "correction_ready") {
    return { ...state, latestCorrection: event.data };
  }

  if (event.event === "done") {
    return { ...state, streamStatus: "idle", statusMessage: "", activeAssistantMessageId: undefined };
  }

  return state;
}

export function failStream(state: ConversationState, errorMessage: string): ConversationState {
  return {
    ...state,
    streamStatus: "error",
    statusMessage: "",
    errorMessage,
    activeAssistantMessageId: undefined,
  };
}
