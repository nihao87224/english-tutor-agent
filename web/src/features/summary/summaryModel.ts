import type { TrainingSessionCompletion } from "../../shared/api";

export interface SummarySection {
  title: string;
  items: string[];
}

export interface SummaryViewModel {
  sessionId: string;
  evidenceCount: number;
  completedTaskCount: number;
  sections: SummarySection[];
}

export function toSummaryViewModel(completion: TrainingSessionCompletion): SummaryViewModel {
  const summary = completion.dailySummary;
  return {
    sessionId: completion.session.sessionId,
    evidenceCount: summary.evidenceCount,
    completedTaskCount: summary.completedTaskCount,
    sections: [
      { title: "Highlights", items: summary.highlights },
      { title: "Memorable expressions", items: summary.memorableItems },
      { title: "Next focus", items: summary.nextFocus },
    ].filter((section) => section.items.length > 0),
  };
}
