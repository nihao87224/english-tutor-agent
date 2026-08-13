import type { TrainingSessionCompletion } from "../../shared/api";
import { toSummaryViewModel } from "./summaryModel";

interface SummaryViewProps {
  completion: TrainingSessionCompletion;
  onBackToHome: () => void;
}

export function SummaryView({ completion, onBackToHome }: SummaryViewProps) {
  const model = toSummaryViewModel(completion);

  return (
    <main className="summary-view">
      <section className="summary-hero">
        <p className="eyebrow">Daily summary</p>
        <h1>Practice completed.</h1>
        <p className="summary">
          {model.completedTaskCount} task completed with {model.evidenceCount} learning evidence item
          {model.evidenceCount === 1 ? "" : "s"}.
        </p>
        <button className="primary-action" type="button" onClick={onBackToHome}>
          Back to today's coach
        </button>
      </section>

      <section className="summary-sections" aria-label="Daily summary sections">
        {model.sections.map((section) => (
          <article className="summary-section" key={section.title}>
            <h2>{section.title}</h2>
            <ul>
              {section.items.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        ))}
      </section>
    </main>
  );
}
