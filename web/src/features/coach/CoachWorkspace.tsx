import { useEffect, useReducer, useState, type FormEvent } from "react";
import type { ApiClient, CurrentTrainingTask, TrainingSessionCompletion } from "../../shared/api";
import type { CoachSelection } from "./TodayCoachHome";
import { CorrectionPanel } from "./CorrectionPanel";
import {
  EMPTY_CONVERSATION_STATE,
  applySseEvent,
  failStream,
  startUserMessage,
  type ConversationState,
} from "./conversationState";

interface CoachWorkspaceProps {
  apiClient: ApiClient;
  userKey: string;
  selection: CoachSelection;
  onBack: () => void;
  onCompleted: (completion: TrainingSessionCompletion) => void;
}

type ConversationAction =
  | { type: "start"; text: string; retry: boolean }
  | { type: "event"; event: Parameters<typeof applySseEvent>[1] }
  | { type: "fail"; message: string };

export function CoachWorkspace({ apiClient, userKey, selection, onBack, onCompleted }: CoachWorkspaceProps) {
  const [taskState, setTaskState] = useState<"loading" | "content" | "error">("loading");
  const [currentTask, setCurrentTask] = useState<CurrentTrainingTask | null>(null);
  const [draft, setDraft] = useState("");
  const [retryMode, setRetryMode] = useState(false);
  const [lastSubmittedText, setLastSubmittedText] = useState("");
  const [completionState, setCompletionState] = useState<"idle" | "completing" | "error">("idle");
  const [completionError, setCompletionError] = useState("");
  const [conversation, dispatch] = useReducer(conversationReducer, EMPTY_CONVERSATION_STATE);
  const userMessageCount = conversation.messages.filter((message) => message.role === "user").length;

  useEffect(() => {
    let cancelled = false;
    async function loadTask() {
      setTaskState("loading");
      try {
        const task = await apiClient.getCurrentTrainingTask(selection.session.sessionId, { userKey });
        if (!cancelled) {
          setCurrentTask(task);
          setTaskState("content");
        }
      } catch {
        if (!cancelled) {
          setTaskState("error");
        }
      }
    }
    void loadTask();
    return () => {
      cancelled = true;
    };
  }, [apiClient, selection.session.sessionId, userKey]);

  async function sendMessage(text: string, retry = false) {
    const trimmed = text.trim();
    if (!trimmed || conversation.streamStatus === "streaming") {
      return;
    }

    setLastSubmittedText(trimmed);
    setDraft("");
    setRetryMode(false);
    dispatch({ type: "start", text: trimmed, retry });

    try {
      await apiClient.streamConversationMessage(
        selection.session.sessionId,
        {
          messageType: "TEXT",
          text: trimmed,
          taskId: currentTask?.taskId ?? selection.task.taskId,
        },
        (event) => dispatch({ type: "event", event }),
        { userKey },
      );
    } catch (error) {
      setDraft(trimmed);
      dispatch({ type: "fail", message: error instanceof Error ? error.message : "Stream failed. Please retry." });
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void sendMessage(draft, retryMode);
  }

  async function completePractice() {
    setCompletionState("completing");
    setCompletionError("");
    try {
      const completion = await apiClient.completeTrainingSession(selection.session.sessionId, { userKey });
      setCompletionState("idle");
      onCompleted(completion);
      return completion;
    } catch (error) {
      setCompletionError(error instanceof Error ? error.message : "Practice completion failed. Please retry.");
      setCompletionState("error");
      return null;
    }
  }

  return (
    <main className="coach-workspace">
      <aside className="workspace-sidebar">
        <button className="text-button" type="button" onClick={onBack}>
          Back
        </button>
        <p className="eyebrow">Today's task</p>
        <h2>{currentTask?.title ?? selection.task.title}</h2>
        <p>{currentTask?.reason ?? selection.task.reason ?? "Practice one idea in natural English."}</p>
        <div className="workspace-facts">
          <span>{selection.session.mode}</span>
          <span>{taskState === "loading" ? "Loading task" : taskState === "error" ? "Task fallback" : "Ready"}</span>
        </div>
        <CompletePracticeButton
          disabled={userMessageCount === 0 || conversation.streamStatus === "streaming" || completionState === "completing"}
          completionState={completionState}
          errorMessage={completionError}
          onComplete={completePractice}
        />
      </aside>

      <section className="conversation-panel" aria-label="Conversation">
        <div className="message-list">
          {conversation.messages.length === 0 ? (
            <div className="empty-chat">
              <strong>Write one sentence or mixed-language idea.</strong>
              <span>Example: I very like this movie because it makes me relax.</span>
            </div>
          ) : (
            conversation.messages.map((message) => (
              <article className={`chat-message ${message.role} ${message.kind === "retry" ? "retry" : ""}`} key={message.id}>
                <span>{message.role === "user" ? (message.kind === "retry" ? "Try Again" : "You") : "Coach"}</span>
                <p>{message.text || (message.role === "assistant" ? "..." : "")}</p>
              </article>
            ))
          )}
        </div>

        {conversation.statusMessage ? <p className="stream-status">{conversation.statusMessage}</p> : null}
        {conversation.streamStatus === "error" ? (
          <div className="form-error">
            {conversation.errorMessage}
            <button className="inline-action" type="button" onClick={() => void sendMessage(lastSubmittedText)}>
              Retry
            </button>
          </div>
        ) : null}

        <form className="composer" onSubmit={handleSubmit}>
          <textarea
            value={draft}
            maxLength={4000}
            onChange={(event) => setDraft(event.target.value)}
            placeholder={retryMode ? "Rewrite it using the suggested pattern..." : "Type your English sentence or Chinese idea..."}
            disabled={conversation.streamStatus === "streaming"}
          />
          <button className="primary-action" type="submit" disabled={!draft.trim() || conversation.streamStatus === "streaming"}>
            {conversation.streamStatus === "streaming" ? "Streaming..." : retryMode ? "Try Again" : "Send"}
          </button>
        </form>
      </section>

      <CorrectionPanel
        correction={conversation.latestCorrection}
        onTryAgain={(cue) => {
          setDraft(cue);
          setRetryMode(true);
        }}
      />
    </main>
  );
}

function CompletePracticeButton({
  disabled,
  completionState,
  errorMessage,
  onComplete,
}: {
  disabled: boolean;
  completionState: "idle" | "completing" | "error";
  errorMessage: string;
  onComplete: () => Promise<TrainingSessionCompletion | null>;
}) {
  return (
    <div className="complete-block">
      <button
        className="secondary-action"
        type="button"
        disabled={disabled}
        onClick={async () => {
          await onComplete();
        }}
      >
        {completionState === "completing" ? "Completing..." : "Complete practice"}
      </button>
      {completionState === "error" ? <p className="form-error">{errorMessage}</p> : null}
    </div>
  );
}

function conversationReducer(state: ConversationState, action: ConversationAction): ConversationState {
  if (action.type === "start") {
    return startUserMessage(state, action.text, createMessageId, action.retry ? "retry" : "initial");
  }
  if (action.type === "event") {
    return applySseEvent(state, action.event);
  }
  return failStream(state, action.message);
}

function createMessageId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `message-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
