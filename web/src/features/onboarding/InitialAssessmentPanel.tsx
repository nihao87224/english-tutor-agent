import { useEffect, useState, type FormEvent } from "react";
import type { ApiClient, AssessmentItem, AssessmentSession } from "../../shared/api";

interface InitialAssessmentPanelProps {
  apiClient: ApiClient;
  onCompleted: () => void;
}

export function InitialAssessmentPanel({ apiClient, onCompleted }: InitialAssessmentPanelProps) {
  const [session, setSession] = useState<AssessmentSession | null>(null);
  const [item, setItem] = useState<AssessmentItem | null>(null);
  const [answer, setAnswer] = useState("");
  const [state, setState] = useState<"loading" | "answering" | "error">("loading");
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const current = await apiClient.getCurrentAssessment();
        const activeSession = current ?? await apiClient.startAssessment();
        const nextItem = await apiClient.getNextAssessmentItem(activeSession.assessmentId);
        if (cancelled) return;
        setSession(activeSession);
        setItem(nextItem ?? null);
        setState("answering");
      } catch (cause) {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : "Unable to load the initial assessment.");
          setState("error");
        }
      }
    }
    void load();
    return () => { cancelled = true; };
  }, [apiClient]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !item || !answer.trim()) return;
    setState("loading");
    try {
      await apiClient.submitAssessmentAnswer(session.assessmentId, {
        itemId: item.itemId,
        answerType: item.type === "MULTIPLE_CHOICE" ? "OPTION" : "TEXT",
        option: item.type === "MULTIPLE_CHOICE" ? answer : null,
        text: item.type === "MULTIPLE_CHOICE" ? null : answer,
      });
      const nextItem = await apiClient.getNextAssessmentItem(session.assessmentId);
      if (nextItem) {
        setItem(nextItem);
        setAnswer("");
        setState("answering");
        return;
      }
      await apiClient.completeAssessment(session.assessmentId);
      onCompleted();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to save this answer.");
      setState("error");
    }
  }

  if (state === "loading") {
    return <main className="onboarding-layout"><section className="onboarding-copy"><p className="eyebrow">Initial assessment</p><h1>Preparing your assessment…</h1></section></main>;
  }
  if (state === "error") {
    return <main className="onboarding-layout"><section className="onboarding-copy"><p className="eyebrow">Initial assessment</p><h1>Assessment is unavailable</h1><p className="summary">{error}</p><button className="secondary-action" type="button" onClick={() => window.location.reload()}>Retry</button></section></main>;
  }
  if (!item) {
    return <main className="onboarding-layout"><section className="onboarding-copy"><p className="eyebrow">Initial assessment</p><h1>Finishing your result…</h1></section></main>;
  }

  return (
    <main className="onboarding-layout">
      <section className="onboarding-copy">
        <p className="eyebrow">Initial assessment · {item.skill}</p>
        <h1>{item.prompt}</h1>
        {item.timeLimitSeconds ? <p className="summary">Suggested time: {item.timeLimitSeconds} seconds</p> : null}
      </section>
      <form className="onboarding-form" onSubmit={submit}>
        {item.type === "MULTIPLE_CHOICE" ? (
          <fieldset>
            <legend>Select one answer</legend>
            {item.options.map((option) => <label className="choice" key={option}><input type="radio" name="answer" value={option.charAt(0)} checked={answer === option.charAt(0)} onChange={() => setAnswer(option.charAt(0))} /><span>{option}</span></label>)}
          </fieldset>
        ) : (
          <label className="field-stack"><span>Your answer</span><textarea value={answer} maxLength={800} rows={6} onChange={(event) => setAnswer(event.target.value)} required /></label>
        )}
        <button className="primary-action" type="submit" disabled={!answer.trim()}>Continue</button>
      </form>
    </main>
  );
}
