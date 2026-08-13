import type { CorrectionReadyEventData, LayeredCorrection } from "../../shared/api";

export interface CorrectionPanelItem {
  original: string;
  corrected: string;
  severity: string;
  why: string;
  naturalExpressions: string[];
  patternCue: string;
}

export interface CorrectionPanelModel {
  status: "waiting" | "no-error" | "has-corrections";
  overallFeedback: string;
  items: CorrectionPanelItem[];
  tryAgainCue?: string;
}

export function toCorrectionPanelModel(correction?: CorrectionReadyEventData): CorrectionPanelModel {
  if (!correction) {
    return {
      status: "waiting",
      overallFeedback: "Corrections will appear after the coach analyzes your expression.",
      items: [],
    };
  }

  const items = correction.corrections.slice(0, 3).map(toPanelItem);
  if (!correction.hasError || items.length === 0) {
    return {
      status: "no-error",
      overallFeedback: correction.overallFeedback || "This expression works. Try making it a little more specific or natural.",
      items: [],
    };
  }

  return {
    status: "has-corrections",
    overallFeedback: correction.overallFeedback,
    items,
    tryAgainCue: createTryAgainCue(items[0]),
  };
}

export function createTryAgainCue(item?: CorrectionPanelItem): string | undefined {
  if (!item) {
    return undefined;
  }
  return item.naturalExpressions[0] ?? item.corrected;
}

function toPanelItem(correction: LayeredCorrection): CorrectionPanelItem {
  const naturalExpressions = correction.naturalSuggestions.map((suggestion) => suggestion.sentence).filter(Boolean);
  return {
    original: correction.original,
    corrected: correction.corrected,
    severity: correction.severity,
    why: correction.explanationZh,
    naturalExpressions,
    patternCue: createPatternCue(correction, naturalExpressions[0]),
  };
}

function createPatternCue(correction: LayeredCorrection, naturalExpression?: string): string {
  if (naturalExpression) {
    return naturalExpression;
  }
  return correction.corrected || correction.original;
}
