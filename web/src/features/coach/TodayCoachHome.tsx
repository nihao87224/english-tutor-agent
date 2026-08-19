import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { ApiClient, LearningPlan, PlanTask, QuotaStatus, TrainingSession } from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import { formatTaskReason, selectExpressionCoachTask } from "./selectExpressionTask";

interface TodayCoachHomeProps {
  apiClient: ApiClient;
  quota: QuotaStatus | null;
  quotaLoading: boolean;
  onRefreshQuota: () => Promise<void>;
  onOpenAccount: () => void;
  onProgressInvalid: () => Promise<void>;
  onStart: (selection: CoachSelection) => void;
}

export interface CoachSelection {
  plan: LearningPlan;
  task: PlanTask;
  session: TrainingSession;
}

type LoadState =
  | { status: "loading" }
  | { status: "content"; plan: LearningPlan }
  | { status: "empty"; plan?: LearningPlan }
  | { status: "error"; message: string };

export function TodayCoachHome({ apiClient, quota, quotaLoading, onRefreshQuota, onOpenAccount, onProgressInvalid, onStart }: TodayCoachHomeProps) {
  const { t } = useI18n();
  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [startState, setStartState] = useState<"idle" | "starting" | "error">("idle");
  const [startError, setStartError] = useState("");

  async function loadPlan() {
    setLoadState({ status: "loading" });
    try {
      const plan = await apiClient.getTodayPlan();
      setLoadState(plan.tasks.length > 0 ? { status: "content", plan } : { status: "empty", plan });
    } catch (error) {
      if (isMissingAssessmentResultError(error)) {
        await onProgressInvalid();
      }
      setLoadState({ status: "error", message: error instanceof Error ? error.message : t("home.error.title") });
    }
  }

  useEffect(() => {
    void loadPlan();
  }, []);

  if (loadState.status === "loading") {
    return <CoachHomeFrame title={t("home.loading.title")} description={t("home.loading.desc")} />;
  }

  if (loadState.status === "error") {
    return (
      <CoachHomeFrame title={t("home.error.title")} description={loadState.message}>
        <button className="secondary-action" type="button" onClick={loadPlan}>
          {t("home.retry")}
        </button>
      </CoachHomeFrame>
    );
  }

  if (loadState.status === "empty") {
    return (
      <CoachHomeFrame title={t("home.empty.title")} description={t("home.empty.desc")}>
        <button className="secondary-action" type="button" onClick={loadPlan}>
          {t("home.refresh")}
        </button>
      </CoachHomeFrame>
    );
  }

  return (
    <CoachHomeContent
      plan={loadState.plan}
      quota={quota}
      quotaLoading={quotaLoading}
      startState={startState}
      startError={startError}
      onRefreshQuota={onRefreshQuota}
      onOpenAccount={onOpenAccount}
      onStart={async (task) => {
        setStartState("starting");
        setStartError("");
        try {
          const session = await apiClient.startTrainingSession({ planId: loadState.plan.planId, mode: "TEXT" });
          onStart({ plan: loadState.plan, task, session });
          setStartState("idle");
        } catch (error) {
          setStartError(error instanceof Error ? error.message : "Training session failed to start. Please retry.");
          setStartState("error");
        }
      }}
    />
  );
}

function isMissingAssessmentResultError(error: unknown): boolean {
  return error instanceof Error && error.message.includes("initial assessment result is required before planning");
}

function CoachHomeContent({
  plan,
  quota,
  quotaLoading,
  startState,
  startError,
  onRefreshQuota,
  onOpenAccount,
  onStart,
}: {
  plan: LearningPlan;
  quota: QuotaStatus | null;
  quotaLoading: boolean;
  startState: "idle" | "starting" | "error";
  startError: string;
  onRefreshQuota: () => Promise<void>;
  onOpenAccount: () => void;
  onStart: (task: PlanTask) => void;
}) {
  const { t } = useI18n();
  const selectedTask = useMemo(() => selectExpressionCoachTask(plan), [plan]);

  if (!selectedTask) {
    return (
      <CoachHomeFrame title={t("home.empty.title")} description={t("home.empty.desc")}>
        <span />
      </CoachHomeFrame>
    );
  }

  return (
    <section className="coach-home">
      <section className="coach-brief">
        <p className="eyebrow">{t("home.eyebrow")}</p>
        <h1>{selectedTask.title}</h1>
        <p className="summary">{formatTaskReason(plan, selectedTask)}</p>
        <div className="coach-meta">
          <span>{selectedTask.durationMinutes} min</span>
          <span>{selectedTask.difficulty}</span>
          <span>{selectedTask.skillFocus.join(" / ") || "expression"}</span>
        </div>
        <button className="primary-action" type="button" disabled={startState === "starting"} onClick={() => onStart(selectedTask)}>
          {startState === "starting" ? t("home.starting") : t("home.start")}
        </button>
        {startState === "error" ? <p className="form-error">{startError}</p> : null}
      </section>

      <aside className="plan-panel" aria-label="Today plan">
        <div className="panel-header">
          <span>{t("home.plan")}</span>
          <strong>{plan.totalMinutes} min</strong>
        </div>
        <ol className="task-list">
          {plan.tasks.map((task) => (
            <li className={task.taskId === selectedTask.taskId ? "active" : ""} key={task.taskId}>
              <span>{task.type}</span>
              <strong>{task.title}</strong>
            </li>
          ))}
        </ol>
        <QuotaBox quota={quota} loading={quotaLoading} onRefreshQuota={onRefreshQuota} onOpenAccount={onOpenAccount} />
      </aside>
    </section>
  );
}

function QuotaBox({
  quota,
  loading,
  onRefreshQuota,
  onOpenAccount,
}: {
  quota: QuotaStatus | null;
  loading: boolean;
  onRefreshQuota: () => Promise<void>;
  onOpenAccount: () => void;
}) {
  const { t } = useI18n();
  return (
    <div className="quota-box">
      <div>
        <span>{t("home.quota")}</span>
        <strong>{quota ? quotaText(quota, t) : loading ? "..." : "-"}</strong>
      </div>
      {quota ? (
        <small>
          {t("home.quotaUsed", { used: quota.used, limit: quota.dailyLimit + quota.bonus })} ·{" "}
          {t("home.quotaReset", { time: new Date(quota.resetAt).toLocaleString() })}
        </small>
      ) : null}
      <div className="quota-actions">
        <button className="text-button" type="button" onClick={() => void onRefreshQuota()}>
          {t("home.refresh")}
        </button>
        <button className="text-button" type="button" onClick={onOpenAccount}>
          {t("app.nav.account")}
        </button>
      </div>
    </div>
  );
}

function quotaText(quota: QuotaStatus, t: (key: string, params?: Record<string, string | number>) => string): string {
  return quota.unlimited ? t("home.quotaUnlimited") : t("home.quotaRemaining", { remaining: quota.remaining });
}

function CoachHomeFrame({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: ReactNode;
}) {
  const { t } = useI18n();
  return (
    <section className="app-shell">
      <section className="hero">
        <p className="eyebrow">{t("home.eyebrow")}</p>
        <h1>{title}</h1>
        <p className="summary">{description}</p>
        {children}
      </section>
    </section>
  );
}
