import type { CorrectionReadyEventData } from "../../shared/api";
import { toCorrectionPanelModel } from "./correctionPanelModel";

interface CorrectionPanelProps {
  correction?: CorrectionReadyEventData;
  onTryAgain?: (cue: string) => void;
}

export function CorrectionPanel({ correction, onTryAgain }: CorrectionPanelProps) {
  const model = toCorrectionPanelModel(correction);

  return (
    <aside className="correction-panel" aria-label="Correction panel">
      <div className="panel-header stacked">
        <span>Correction</span>
        <strong>{model.status === "has-corrections" ? "Expression upgrades" : model.status === "no-error" ? "Looks good" : "Waiting"}</strong>
      </div>

      <p className="panel-feedback">{model.overallFeedback}</p>

      {model.tryAgainCue ? (
        <button className="try-again-action" type="button" onClick={() => onTryAgain?.(model.tryAgainCue!)}>
          Try Again
        </button>
      ) : null}

      {model.items.length > 0 ? (
        <div className="correction-list">
          {model.items.map((item) => (
            <article className="correction-card" key={`${item.original}-${item.corrected}`}>
              <div className="correction-pair">
                <span>Instead of</span>
                <p>{item.original}</p>
                <span>Say</span>
                <p>{item.corrected}</p>
              </div>
              <div>
                <h3>Why</h3>
                <p>{item.why}</p>
              </div>
              {item.naturalExpressions.length > 0 ? (
                <div>
                  <h3>Natural</h3>
                  <ul>
                    {item.naturalExpressions.map((sentence) => (
                      <li key={sentence}>{sentence}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <div>
                <h3>Pattern</h3>
                <p>{item.patternCue}</p>
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </aside>
  );
}
