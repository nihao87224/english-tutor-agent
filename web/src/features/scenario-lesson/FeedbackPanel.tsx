import { useEffect, useState } from "react";
import type { LessonAttemptReceipt, LessonFeedbackCompletion } from "../../shared/api";
import { useI18n } from "../../shared/i18n";

export function FeedbackPanel({ attempt: initialAttempt, evidenceSummary, onGetAttempt, onComplete }: {
  attempt?: LessonAttemptReceipt; evidenceSummary?: LessonFeedbackCompletion;
  onGetAttempt: (attemptId: string) => Promise<LessonAttemptReceipt>; onComplete: (attemptId: string) => Promise<void>;
}) {
  const { t } = useI18n();
  const [attempt, setAttempt] = useState(initialAttempt);
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  useEffect(() => { setAttempt(initialAttempt); }, [initialAttempt]);
  useEffect(() => {
    if (!attempt || attempt.status !== "ANALYSIS_PENDING") return;
    const timer = window.setInterval(() => {
      void onGetAttempt(attempt.attemptId).then(setAttempt).catch(() => setError(t("lesson.feedback.refreshError")));
    }, attempt.pollAfterMs ?? 1000);
    return () => window.clearInterval(timer);
  }, [attempt, onGetAttempt, t]);
  if (!attempt) return <section><h1 id="scenario-lesson-title">{t("lesson.feedback.title")}</h1><p role="status">{t("lesson.attempt.pending")}</p></section>;
  if (evidenceSummary) return <section aria-labelledby="scenario-lesson-title"><h1 id="scenario-lesson-title">{t("lesson.feedback.complete")}</h1>
    <p>{t("lesson.feedback.evidence", { count: evidenceSummary.evidenceCount })}</p><p>{evidenceSummary.nextFocus}</p></section>;
  if (attempt.status === "ANALYSIS_PENDING" || attempt.status === "ANALYSIS_RETRYABLE") return <section><h1 id="scenario-lesson-title">{t("lesson.feedback.title")}</h1>
    <p role="status">{t("lesson.attempt.pending")}</p>{attempt.status === "ANALYSIS_RETRYABLE" ? <p>{t("lesson.feedback.retrying")}</p> : null}</section>;
  if (attempt.status === "ANALYSIS_FAILED") return <section><h1 id="scenario-lesson-title">{t("lesson.feedback.title")}</h1><p role="alert">{t("lesson.feedback.failed")}</p></section>;
  if (attempt.status === "RETRY_REQUIRED") return <section><h1 id="scenario-lesson-title">{t("lesson.feedback.retryTitle")}</h1><AnalysisDetails attempt={attempt} /><p>{t("lesson.feedback.retryHint")}</p></section>;
  return <section aria-labelledby="scenario-lesson-title"><h1 id="scenario-lesson-title">{t("lesson.feedback.title")}</h1><AnalysisDetails attempt={attempt} />
    {error ? <p role="alert">{error}</p> : null}<button className="scenario-primary-action" type="button" disabled={saving} onClick={() => {
      setSaving(true); setError(undefined); void onComplete(attempt.attemptId).catch((reason) => setError(reason instanceof Error ? reason.message : t("lesson.feedback.refreshError"))).finally(() => setSaving(false));
    }}>{saving ? t("lesson.attempt.submitting") : t("lesson.feedback.finish")}</button></section>;
}

function AnalysisDetails({ attempt }: { attempt: LessonAttemptReceipt }) {
  const analysis = attempt.analysis;
  if (!analysis) return null;
  return <div className="lesson-feedback" aria-live="polite"><p>{analysis.summary}</p>
    {analysis.corrections.length > 0 ? <ul>{analysis.corrections.map((correction) => <li key={`${correction.sourceText}-${correction.suggestedText}`}><strong>{correction.sourceText} → {correction.suggestedText}</strong><span>{correction.explanation}</span></li>)}</ul> : null}
    {analysis.naturalExpressions.length > 0 ? <section><h2>Natural expressions</h2><ul>{analysis.naturalExpressions.map((value) => <li key={value}>{value}</li>)}</ul></section> : null}
  </div>;
}
