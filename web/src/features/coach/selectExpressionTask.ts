import type { LearningPlan, PlanTask } from "../../shared/api";

const EXPRESSION_TASK_TYPES = new Set(["CONVERSATION", "SPEAKING", "WRITING"]);

export function selectExpressionCoachTask(plan: LearningPlan): PlanTask | undefined {
  return plan.tasks.find((task) => EXPRESSION_TASK_TYPES.has(task.type)) ?? plan.tasks[0];
}

export function formatTaskReason(plan: LearningPlan, task: PlanTask): string {
  return task.reason ?? plan.reasons[0] ?? "Today is a good day to turn one idea into natural English.";
}
