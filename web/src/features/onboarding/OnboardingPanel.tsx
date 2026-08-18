import { useState, type FormEvent } from "react";
import type { ApiClient, CorrectionStyle, PreferenceRequest, PrimaryGoal, SelfAssessmentRequest } from "../../shared/api";
import type { LocalOnboardingState } from "../../shared/session/localSession";
import { useI18n } from "../../shared/i18n";

interface OnboardingPanelProps {
  apiClient: ApiClient;
  initialState: LocalOnboardingState;
  step: "GOAL" | "PREFERENCES" | "SELF_ASSESSMENT";
  onProgressChanged: () => void;
}

const goals: PrimaryGoal[] = ["WORKPLACE", "GENERAL", "IELTS"];
const minutes: PreferenceRequest["dailyMinutes"][] = [5, 10, 20, 30, 45];
const styles: CorrectionStyle[] = ["LIGHT", "STANDARD", "STRICT"];

const goalLabelKeys: Record<PrimaryGoal, { label: string; description: string }> = {
  WORKPLACE: { label: "onboarding.goal.workplace", description: "onboarding.goal.workplace.desc" },
  GENERAL: { label: "onboarding.goal.general", description: "onboarding.goal.general.desc" },
  IELTS: { label: "onboarding.goal.ielts", description: "onboarding.goal.ielts.desc" },
};

export function OnboardingPanel({ apiClient, initialState, step, onProgressChanged }: OnboardingPanelProps) {
  const { t } = useI18n();
  const [state, setState] = useState(initialState);
  const [submitState, setSubmitState] = useState<"idle" | "saving" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState("");

  function updateState(nextState: LocalOnboardingState) {
    setState(nextState);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitState("saving");
    setErrorMessage("");

    try {
      await apiClient.putPrimaryGoal(state.primaryGoal);
      await apiClient.putPreferences({
        dailyMinutes: state.dailyMinutes,
        correctionStyle: state.correctionStyle,
        reminderEnabled: false,
        saveRawText: state.saveRawText,
        saveRawAudio: false,
      });

      onProgressChanged();
      setSubmitState("idle");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : t("onboarding.error"));
      setSubmitState("error");
    }
  }

  if (step === "SELF_ASSESSMENT") {
    return <SelfAssessmentPanel apiClient={apiClient} onProgressChanged={onProgressChanged} />;
  }

  return (
    <main className="onboarding-layout">
      <section className="onboarding-copy">
        <p className="eyebrow">{t("onboarding.eyebrow")}</p>
        <h1>{t("onboarding.title")}</h1>
        <p className="summary">{t("onboarding.summary")}</p>
      </section>

      <form className="onboarding-form" onSubmit={handleSubmit}>
        <fieldset>
          <legend>{t("onboarding.goal")}</legend>
          <div className="option-grid">
            {goals.map((goal) => (
              <label className="choice" key={goal}>
                <input
                  type="radio"
                  name="primaryGoal"
                  value={goal}
                  checked={state.primaryGoal === goal}
                  onChange={() => updateState({ ...state, primaryGoal: goal })}
                />
                <span>
                  <strong>{t(goalLabelKeys[goal].label)}</strong>
                  <small>{t(goalLabelKeys[goal].description)}</small>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>{t("onboarding.minutes")}</legend>
          <div className="segmented-control">
            {minutes.map((value) => (
              <button
                type="button"
                key={value}
                className={state.dailyMinutes === value ? "selected" : ""}
                onClick={() => updateState({ ...state, dailyMinutes: value })}
              >
                {value}
              </button>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>{t("onboarding.style")}</legend>
          <div className="segmented-control">
            {styles.map((value) => (
              <button
                type="button"
                key={value}
                className={state.correctionStyle === value ? "selected" : ""}
                onClick={() => updateState({ ...state, correctionStyle: value })}
              >
                {value}
              </button>
            ))}
          </div>
        </fieldset>

        <label className="toggle-row">
          <input
            type="checkbox"
            checked={state.saveRawText}
            onChange={(event) => updateState({ ...state, saveRawText: event.target.checked })}
          />
          <span>{t("onboarding.saveRawText")}</span>
        </label>

        {submitState === "error" ? <p className="form-error">{errorMessage}</p> : null}

        <button className="primary-action" type="submit" disabled={submitState === "saving"}>
          {submitState === "saving" ? t("onboarding.saving") : t("onboarding.submit")}
        </button>
      </form>
    </main>
  );
}

function SelfAssessmentPanel({ apiClient, onProgressChanged }: { apiClient: ApiClient; onProgressChanged: () => void }) {
  const [ratings, setRatings] = useState<SelfAssessmentRequest>({
    listening: "INTERMEDIATE",
    speaking: "INTERMEDIATE",
    reading: "INTERMEDIATE",
    writing: "INTERMEDIATE",
  });
  const [submitState, setSubmitState] = useState<"idle" | "saving" | "error">("idle");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitState("saving");
    try {
      await apiClient.submitSelfAssessment(ratings);
      onProgressChanged();
    } catch {
      setSubmitState("error");
    }
  }

  return (
    <main className="onboarding-layout">
      <section className="onboarding-copy">
        <p className="eyebrow">Initial assessment</p>
        <h1>Tell us your current level</h1>
        <p className="summary">This helps us choose an appropriate starting point before the short initial assessment.</p>
      </section>
      <form className="onboarding-form" onSubmit={submit}>
        {(["listening", "speaking", "reading", "writing"] as const).map((skill) => (
          <label className="field-stack" key={skill}>
            <span>{skill[0].toUpperCase() + skill.slice(1)}</span>
            <select value={ratings[skill]} onChange={(event) => setRatings({ ...ratings, [skill]: event.target.value as SelfAssessmentRequest[typeof skill] })}>
              <option value="BEGINNER">Beginner</option>
              <option value="BASIC">Basic</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="UPPER_INTERMEDIATE">Upper intermediate</option>
              <option value="ADVANCED">Advanced</option>
            </select>
          </label>
        ))}
        {submitState === "error" ? <p className="form-error">Unable to save your self-assessment. Please retry.</p> : null}
        <button className="primary-action" type="submit" disabled={submitState === "saving"}>
          {submitState === "saving" ? "Saving..." : "Continue to initial assessment"}
        </button>
      </form>
    </main>
  );
}
