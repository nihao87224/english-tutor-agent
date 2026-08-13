import { describe, expect, it } from "vitest";
import type { LearningPlan } from "../../shared/api";
import { formatTaskReason, selectExpressionCoachTask } from "./selectExpressionTask";

describe("selectExpressionCoachTask", () => {
  it("prefers conversation, speaking or writing tasks", () => {
    const task = selectExpressionCoachTask({
      ...basePlan(),
      tasks: [
        { taskId: "r1", type: "REVIEW", title: "Review", durationMinutes: 5, skillFocus: [], difficulty: "EASY" },
        { taskId: "c1", type: "CONVERSATION", title: "Explain a work problem", durationMinutes: 10, skillFocus: [], difficulty: "MEDIUM" },
      ],
    });

    expect(task?.taskId).toBe("c1");
  });

  it("falls back to the first task when the plan has no expression-specific task", () => {
    const task = selectExpressionCoachTask({
      ...basePlan(),
      tasks: [{ taskId: "r1", type: "REVIEW", title: "Review", durationMinutes: 5, skillFocus: [], difficulty: "EASY" }],
    });

    expect(task?.taskId).toBe("r1");
  });

  it("returns undefined for an empty plan", () => {
    expect(selectExpressionCoachTask({ ...basePlan(), tasks: [] })).toBeUndefined();
  });

  it("uses task reason before plan reason", () => {
    const plan = basePlan();
    const task = {
      taskId: "w1",
      type: "WRITING" as const,
      title: "Improve one sentence",
      durationMinutes: 5,
      skillFocus: ["expression"],
      difficulty: "EASY" as const,
      reason: "You often translate directly from Chinese.",
    };

    expect(formatTaskReason(plan, task)).toBe("You often translate directly from Chinese.");
  });
});

function basePlan(): LearningPlan {
  return {
    planId: "plan-1",
    date: "2026-08-10",
    totalMinutes: 10,
    tasks: [],
    reasons: ["Your next focus is natural workplace expression."],
  };
}
