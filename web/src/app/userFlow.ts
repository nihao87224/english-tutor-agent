import type { UserLearningProgress } from "../shared/api";

export type LearnerScreen = "onboarding" | "assessment" | "coach";

export function resolveLearnerScreen(progress: UserLearningProgress): LearnerScreen {
  switch (progress.nextStep) {
    case "ONBOARDING_REQUIRED":
      return "onboarding";
    case "ASSESSMENT_REQUIRED":
      return "assessment";
    case "READY_FOR_PLAN":
      return "coach";
  }
}
