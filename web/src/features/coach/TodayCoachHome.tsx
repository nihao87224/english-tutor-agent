import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { ApiClient, LearningPlan, PlanTask, TrainingSession } from "../../shared/api";
import { formatTaskReason, selectExpressionCoachTask } from "./selectExpressionTask";

interface TodayCoachHomeProps {
  apiClient: ApiClient;
  userKey: string;
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

export function TodayCoachHome({ apiClient, userKey, onStart }: TodayCoachHomeProps) {
  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [startState, setStartState] = useState<"idle" | "starting" | "error">("idle");
  const [startError, setStartError] = useState("");

  async function loadPlan() {
    setLoadState({ status: "loading" });
    try {
      const plan = await apiClient.getTodayPlan({ userKey });
      setLoadState(plan.tasks.length > 0 ? { status: "content", plan } : { status: "empty", plan });
    } catch (error) {
      setLoadState({ status: "error", message: error instanceof Error ? error.message : "Today's plan failed to load." });
    }
  }

  useEffect(() => {
    void loadPlan();
  }, []);

  if (loadState.status === "loading") {
    return <CoachHomeFrame title="Loading today's coach" description="Reading today's expression plan..." />;
  }

  if (loadState.status === "error") {
    return (
      <CoachHomeFrame title="Today's coach is unavailable" description={loadState.message}>
        <button className="secondary-action" type="button" onClick={loadPlan}>
          Retry
        </button>
      </CoachHomeFrame>
    );
  }

  if (loadState.status === "empty") {
    return (
      <CoachHomeFrame
        title="No expression task yet"
        description="Today's plan does not have a practice task yet. Refresh and try again."
      >
        <button className="secondary-action" type="button" onClick={loadPlan}>
          Refresh
        </button>
      </CoachHomeFrame>
    );
  }

  return (
    <CoachHomeContent
      plan={loadState.plan}
      startState={startState}
      startError={startError}
      onStart={async (task) => {
        setStartState("starting");
        setStartError("");
        try {
          const session = await apiClient.startTrainingSession({ planId: loadState.plan.planId, mode: "TEXT" }, { userKey });
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

function CoachHomeContent({
  plan,
  startState,
  startError,
  onStart,
}: {
  plan: LearningPlan;
  startState: "idle" | "starting" | "error";
  startError: string;
  onStart: (task: PlanTask) => void;
}) {
  const selectedTask = useMemo(() => selectExpressionCoachTask(plan), [plan]);

  if (!selectedTask) {
    return (
      <CoachHomeFrame title="No expression task yet" description="Today's plan does not have a practice task yet.">
        <span />
      </CoachHomeFrame>
    );
  }

  return (
    <main className="coach-home">
      <section className="coach-brief">
        <p className="eyebrow">Today's expression coach</p>
        <h1>{selectedTask.title}</h1>
        <p className="summary">{formatTaskReason(plan, selectedTask)}</p>
        <div className="coach-meta">
          <span>{selectedTask.durationMinutes} min</span>
          <span>{selectedTask.difficulty}</span>
          <span>{selectedTask.skillFocus.join(" / ") || "expression"}</span>
        </div>
        <button className="primary-action" type="button" disabled={startState === "starting"} onClick={() => onStart(selectedTask)}>
          {startState === "starting" ? "Starting..." : "Start practice"}
        </button>
        {startState === "error" ? <p className="form-error">{startError}</p> : null}
      </section>

      <aside className="plan-panel" aria-label="Today plan">
        <div className="panel-header">
          <span>Plan</span>
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
      </aside>
    </main>
  );
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
  return (
    <main className="app-shell">
      <section className="hero">
        <p className="eyebrow">Today's expression coach</p>
        <h1>{title}</h1>
        <p className="summary">{description}</p>
        {children}
      </section>
    </main>
  );
}
