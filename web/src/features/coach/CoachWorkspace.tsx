import { useEffect, useReducer, useState, type FormEvent } from "react";
import { ApiError, type ApiClient, type CurrentTrainingTask, type QuotaStatus, type TrainingSessionCompletion } from "../../shared/api";
import { useI18n } from "../../shared/i18n";
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
  quota: QuotaStatus | null;
  selection: CoachSelection;
  onBack: () => void;
  onQuotaChanged: () => Promise<void>;
  onCompleted: (completion: TrainingSessionCompletion) => void;
}

type ConversationAction =
  | { type: "start"; text: string; retry: boolean }
  | { type: "event"; event: Parameters<typeof applySseEvent>[1] }
  | { type: "fail"; message: string };

export function CoachWorkspace({ apiClient, quota, selection, onBack, onQuotaChanged, onCompleted }: CoachWorkspaceProps) {
  const { t } = useI18n();
  const [taskState, setTaskState] = useState<"loading" | "content" | "error">("loading");
  const [currentTask, setCurrentTask] = useState<CurrentTrainingTask | null>(null);
  const [draft, setDraft] = useState("");
  const [retryMode, setRetryMode] = useState(false);
  const [lastSubmittedText, setLastSubmittedText] = useState("");
  const [completionState, setCompletionState] = useState<"idle" | "completing" | "error">("idle");
  const [completionError, setCompletionError] = useState("");
  const [conversation, dispatch] = useReducer(conversationReducer, EMPTY_CONVERSATION_STATE);
  const userMessageCount = conversation.messages.filter((message) => message.role === "user").length;
  const quotaExhausted = quota ? !quota.unlimited && quota.remaining <= 0 : false;

  useEffect(() => {
    let cancelled = false;
    async function loadTask() {
      setTaskState("loading");
      try {
        const task = await apiClient.getCurrentTrainingTask(selection.session.sessionId);
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
  }, [apiClient, selection.session.sessionId]);

  async function sendMessage(text: string, retry = false) {
    const trimmed = text.trim();
    if (!trimmed || conversation.streamStatus === "streaming" || quotaExhausted) {
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
      );
      await onQuotaChanged();
    } catch (error) {
      setDraft(trimmed);
      dispatch({ type: "fail", message: quotaErrorMessage(error, t("coach.quotaExceeded")) });
      await onQuotaChanged();
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
      const completion = await apiClient.completeTrainingSession(selection.session.sessionId);
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
          {t("coach.back")}
        </button>
        <p className="eyebrow">{t("coach.task")}</p>
        <h2>{currentTask?.title ?? selection.task.title}</h2>
        <p>{currentTask?.reason ?? selection.task.reason ?? "Practice one idea in natural English."}</p>
        <div className="workspace-facts">
          <span>{selection.session.mode}</span>
          <span>{taskState === "loading" ? t("coach.loadingTask") : taskState === "error" ? t("coach.taskFallback") : t("coach.ready")}</span>
          {quota ? <span>{quota.unlimited ? t("home.quotaUnlimited") : t("home.quotaRemaining", { remaining: quota.remaining })}</span> : null}
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
              <strong>{t("coach.empty.title")}</strong>
              <span>{t("coach.empty.example")}</span>
            </div>
          ) : (
            conversation.messages.map((message) => (
              <article className={`chat-message ${message.role} ${message.kind === "retry" ? "retry" : ""}`} key={message.id}>
                <span>{message.role === "user" ? (message.kind === "retry" ? t("coach.tryAgain") : "You") : "Coach"}</span>
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
              {t("coach.retry")}
            </button>
          </div>
        ) : null}

        {quotaExhausted ? <p className="form-error quota-warning">{t("coach.quotaExceeded")}</p> : null}

        <form className="composer" onSubmit={handleSubmit}>
          <textarea
            value={draft}
            maxLength={4000}
            onChange={(event) => setDraft(event.target.value)}
            placeholder={retryMode ? t("coach.retryPlaceholder") : t("coach.placeholder")}
            disabled={conversation.streamStatus === "streaming" || quotaExhausted}
          />
          <button className="primary-action" type="submit" disabled={!draft.trim() || conversation.streamStatus === "streaming" || quotaExhausted}>
            {conversation.streamStatus === "streaming" ? t("coach.streaming") : retryMode ? t("coach.tryAgain") : t("coach.send")}
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
  const { t } = useI18n();
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
        {completionState === "completing" ? t("coach.completing") : t("coach.complete")}
      </button>
      {completionState === "error" ? <p className="form-error">{errorMessage}</p> : null}
    </div>
  );
}

function quotaErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError && (error.status === 429 || error.problem?.code === "DAILY_QUOTA_EXCEEDED")) {
    return error.problem?.detail ?? fallback;
  }
  return error instanceof Error ? error.message : "Stream failed. Please retry.";
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
