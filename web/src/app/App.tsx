import { useMemo, useState } from "react";
import { createApiClient } from "../shared/api";
import { CoachWorkspace } from "../features/coach/CoachWorkspace";
import { TodayCoachHome, type CoachSelection } from "../features/coach/TodayCoachHome";
import { SummaryView } from "../features/summary/SummaryView";
import type { TrainingSessionCompletion } from "../shared/api";
import { OnboardingPanel } from "../features/onboarding/OnboardingPanel";
import {
  getOrCreateUserKey,
  loadOnboardingState,
  saveOnboardingState,
  type LocalOnboardingState,
} from "../shared/session/localSession";

export function App() {
  const [userKey] = useState(() => getOrCreateUserKey());
  const [onboardingState, setOnboardingState] = useState(() => loadOnboardingState());
  const [coachSelection, setCoachSelection] = useState<CoachSelection | null>(null);
  const [completion, setCompletion] = useState<TrainingSessionCompletion | null>(null);
  const apiClient = useMemo(() => createApiClient({ userKey }), [userKey]);

  function persistOnboardingState(nextState: LocalOnboardingState) {
    saveOnboardingState(nextState);
    setOnboardingState(nextState);
  }

  if (!onboardingState.completed) {
    return (
      <OnboardingPanel
        apiClient={apiClient}
        userKey={userKey}
        initialState={onboardingState}
        onStateChange={persistOnboardingState}
        onComplete={persistOnboardingState}
      />
    );
  }

  if (completion) {
    return (
      <SummaryView
        completion={completion}
        onBackToHome={() => {
          setCompletion(null);
          setCoachSelection(null);
        }}
      />
    );
  }

  if (coachSelection) {
    return (
      <CoachWorkspace
        apiClient={apiClient}
        userKey={userKey}
        selection={coachSelection}
        onBack={() => setCoachSelection(null)}
        onCompleted={setCompletion}
      />
    );
  }

  return <TodayCoachHome apiClient={apiClient} userKey={userKey} onStart={setCoachSelection} />;
}
