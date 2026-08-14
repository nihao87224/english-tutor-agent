import type { TrainingSessionCompletion } from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import { toSummaryViewModel } from "./summaryModel";

interface SummaryViewProps {
  completion: TrainingSessionCompletion;
  onBackToHome: () => void;
}

export function SummaryView({ completion, onBackToHome }: SummaryViewProps) {
  const { t } = useI18n();
  const model = toSummaryViewModel(completion);

  return (
    <main className="summary-view">
      <section className="summary-hero">
        <p className="eyebrow">{t("summary.eyebrow")}</p>
        <h1>{t("summary.title")}</h1>
        <p className="summary">{t("summary.body", { tasks: model.completedTaskCount, evidence: model.evidenceCount })}</p>
        <button className="primary-action" type="button" onClick={onBackToHome}>
          {t("summary.back")}
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
