import { useState, type FormEvent } from "react";
import type { ApiClient, CorrectionStyle, PreferenceRequest, PrimaryGoal } from "../../shared/api";
import type { LocalOnboardingState } from "../../shared/session/localSession";

interface OnboardingPanelProps {
  apiClient: ApiClient;
  userKey: string;
  initialState: LocalOnboardingState;
  onStateChange: (state: LocalOnboardingState) => void;
  onComplete: (state: LocalOnboardingState) => void;
}

const goals: Array<{ value: PrimaryGoal; label: string; description: string }> = [
  { value: "WORKPLACE", label: "Workplace", description: "会议、汇报和工作沟通" },
  { value: "GENERAL", label: "General", description: "日常表达和自然说法" },
  { value: "IELTS", label: "IELTS", description: "先按普通表达练，专项后续开启" },
];

const minutes: PreferenceRequest["dailyMinutes"][] = [5, 10, 20, 30, 45];
const styles: CorrectionStyle[] = ["LIGHT", "STANDARD", "STRICT"];

export function OnboardingPanel({ apiClient, userKey, initialState, onStateChange, onComplete }: OnboardingPanelProps) {
  const [state, setState] = useState(initialState);
  const [submitState, setSubmitState] = useState<"idle" | "saving" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState("");

  function updateState(nextState: LocalOnboardingState) {
    setState(nextState);
    onStateChange(nextState);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitState("saving");
    setErrorMessage("");

    try {
      await apiClient.putPrimaryGoal(state.primaryGoal, { userKey });
      await apiClient.putPreferences(
        {
          dailyMinutes: state.dailyMinutes,
          correctionStyle: state.correctionStyle,
          reminderEnabled: false,
          saveRawText: state.saveRawText,
          saveRawAudio: false,
        },
        { userKey },
      );

      const completed = { ...state, completed: true };
      updateState(completed);
      onComplete(completed);
      setSubmitState("idle");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "保存失败，请稍后重试。");
      setSubmitState("error");
    }
  }

  return (
    <main className="onboarding-layout">
      <section className="onboarding-copy">
        <p className="eyebrow">First minute setup</p>
        <h1>先把表达教练跑起来。</h1>
        <p className="summary">
          只需要选择目标、每天练多久和纠错强度。开发期会在本机保存一个 `X-User-Key`，让后端能识别你的学习状态。
        </p>
        <div className="user-key-strip">
          <span>Local user key</span>
          <strong>{userKey}</strong>
        </div>
      </section>

      <form className="onboarding-form" onSubmit={handleSubmit}>
        <fieldset>
          <legend>Goal</legend>
          <div className="option-grid">
            {goals.map((goal) => (
              <label className="choice" key={goal.value}>
                <input
                  type="radio"
                  name="primaryGoal"
                  value={goal.value}
                  checked={state.primaryGoal === goal.value}
                  onChange={() => updateState({ ...state, primaryGoal: goal.value })}
                />
                <span>
                  <strong>{goal.label}</strong>
                  <small>{goal.description}</small>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>Daily minutes</legend>
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
          <legend>Correction style</legend>
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
          <span>Save raw text for better expression feedback</span>
        </label>

        {submitState === "error" ? <p className="form-error">{errorMessage}</p> : null}

        <button className="primary-action" type="submit" disabled={submitState === "saving"}>
          {submitState === "saving" ? "Saving..." : "Enter today's coach"}
        </button>
      </form>
    </main>
  );
}
