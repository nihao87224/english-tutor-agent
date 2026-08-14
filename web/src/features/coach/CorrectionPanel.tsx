import type { CorrectionReadyEventData } from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import { toCorrectionPanelModel } from "./correctionPanelModel";

interface CorrectionPanelProps {
  correction?: CorrectionReadyEventData;
  onTryAgain?: (cue: string) => void;
}

export function CorrectionPanel({ correction, onTryAgain }: CorrectionPanelProps) {
  const { t } = useI18n();
  const model = toCorrectionPanelModel(correction);

  return (
    <aside className="correction-panel" aria-label="Correction panel">
      <div className="panel-header stacked">
        <span>{t("correction.title")}</span>
        <strong>{model.status === "has-corrections" ? t("correction.upgrades") : model.status === "no-error" ? t("correction.good") : t("correction.waiting")}</strong>
      </div>

      <p className="panel-feedback">{model.overallFeedback}</p>

      {model.tryAgainCue ? (
        <button className="try-again-action" type="button" onClick={() => onTryAgain?.(model.tryAgainCue!)}>
          {t("coach.tryAgain")}
        </button>
      ) : null}

      {model.items.length > 0 ? (
        <div className="correction-list">
          {model.items.map((item) => (
            <article className="correction-card" key={`${item.original}-${item.corrected}`}>
              <div className="correction-pair">
                <span>{t("correction.instead")}</span>
                <p>{item.original}</p>
                <span>{t("correction.say")}</span>
                <p>{item.corrected}</p>
              </div>
              <div>
                <h3>{t("correction.why")}</h3>
                <p>{item.why}</p>
              </div>
              {item.naturalExpressions.length > 0 ? (
                <div>
                  <h3>{t("correction.natural")}</h3>
                  <ul>
                    {item.naturalExpressions.map((sentence) => (
                      <li key={sentence}>{sentence}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <div>
                <h3>{t("correction.pattern")}</h3>
                <p>{item.patternCue}</p>
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </aside>
  );
}
