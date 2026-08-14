export type PrimaryGoal = "WORKPLACE" | "GENERAL" | "IELTS";
export type CorrectionStyle = "LIGHT" | "STANDARD" | "STRICT";
export type TrainingSessionMode = "TEXT" | "VOICE" | "MIXED";
export type TrainingSessionType = "DAILY" | "FREE_CONVERSATION" | "REVIEW" | "IELTS";
export type TrainingSessionStatus =
  | "CREATED"
  | "IN_PROGRESS"
  | "PAUSED"
  | "COMPLETING"
  | "COMPLETED"
  | "COMPLETION_FAILED"
  | "CANCELLED";

export interface AuthUser {
  userKey: string;
  email: string;
  status: "ACTIVE" | "DISABLED" | string;
  roles: string[];
  locale: string;
  timezone: string;
}

export interface AuthResponse {
  user: AuthUser;
  accessToken: string;
  expiresIn: number;
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface ProfileSummary {
  primaryGoal: PrimaryGoal;
  dailyMinutes: number;
  correctionStyle: CorrectionStyle;
  onboardingCompleted?: boolean;
}

export interface PreferenceRequest {
  dailyMinutes: 5 | 10 | 20 | 30 | 45;
  correctionStyle: CorrectionStyle;
  reminderEnabled: boolean;
  saveRawText: boolean;
  saveRawAudio: boolean;
}

export interface OnboardingProgress {
  step: "GOAL" | "PREFERENCES" | "SELF_ASSESSMENT" | "ASSESSMENT" | "RESULT" | "COMPLETE";
  completed: boolean;
  assessmentId?: string | null;
}

export interface PrivacySettings {
  saveRawText: boolean;
  saveRawAudio: boolean;
  rawAudioRetentionDays?: number;
}

export interface QuotaStatus {
  quotaDate: string;
  dailyLimit: number;
  used: number;
  bonus: number;
  remaining: number;
  unlimited: boolean;
  resetAt: string;
}

export interface LearningPlan {
  planId: string;
  date: string;
  totalMinutes: number;
  tasks: PlanTask[];
  reasons: string[];
  temporaryAdjustment?: boolean;
}

export interface PlanTask {
  taskId: string;
  type: "REVIEW" | "LISTENING" | "SPEAKING" | "READING" | "WRITING" | "CONVERSATION" | "IELTS" | "SUMMARY";
  title: string;
  durationMinutes: number;
  skillFocus: string[];
  difficulty: "EASY" | "MEDIUM" | "HARD";
  reason?: string;
}

export interface PlanAdjustmentRequest {
  availableMinutes?: 5 | 10 | 20 | 30 | 45;
  preferredSkill?: string | null;
  temporaryScenario?: string | null;
  textOnly?: boolean;
}

export interface TrainingSession {
  sessionId: string;
  planId: string;
  type: TrainingSessionType;
  mode: TrainingSessionMode;
  status: TrainingSessionStatus;
  currentTaskId: string;
  startedAt: string;
  pausedAt?: string | null;
  completedAt?: string | null;
  effectiveSeconds: number;
}

export interface StartTrainingSessionRequest {
  planId: string;
  mode?: TrainingSessionMode | null;
}

export interface CurrentTrainingTask {
  taskId: string;
  type: string;
  title: string;
  durationMinutes: number;
  skillFocus: string[];
  difficulty: string;
  reason: string;
  status: "READY" | "STARTED" | "COMPLETED" | "SKIPPED";
}

export interface TaskAttemptRequest {
  inputType: "TEXT" | "AUDIO" | "OPTION";
  text?: string | null;
  audioAssetId?: string | null;
  option?: string | null;
  hintLevel?: number | null;
  clientDurationMs?: number | null;
  clientStartedAt?: string | null;
  clientCompletedAt?: string | null;
}

export interface TaskAttemptReceipt {
  attemptId: string;
  status: "ACCEPTED" | "PROCESSING" | "COMPLETED" | "FAILED";
  feedbackAvailable: boolean;
  evidenceCount: number;
}

export interface DailyTrainingSummary {
  sessionId: string;
  completedTaskCount: number;
  evidenceCount: number;
  practicedSkills: string[];
  highlights: string[];
  memorableItems: string[];
  nextFocus: string[];
  generatedAt: string;
}

export interface TrainingSessionCompletion {
  session: TrainingSession;
  dailySummary: DailyTrainingSummary;
}

export interface ConversationMessageRequest {
  messageType: "TEXT" | "AUDIO";
  text: string;
  taskId?: string | null;
}

export interface StatusEventData {
  stage: string;
  message: string;
}

export interface TextDeltaEventData {
  delta: string;
}

export interface NaturalSuggestion {
  sentence: string;
  style: string;
}

export interface LayeredCorrection {
  original: string;
  corrected: string;
  errorType: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | string;
  explanationZh: string;
  shouldInterrupt: boolean;
  memoryWorthy: boolean;
  naturalSuggestions: NaturalSuggestion[];
}

export interface CorrectionReadyEventData {
  hasError: boolean;
  corrections: LayeredCorrection[];
  overallFeedback: string;
  promptVersion: string;
  schemaVersion: string;
  traceId: string;
  providerId: string;
  modelId: string;
}

export interface DoneEventData {
  traceId: string;
  providerId: string;
  modelId: string;
}

export interface ProblemResponse {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  traceId?: string;
  code?: string;
  dailyLimit?: number;
  used?: number;
  remaining?: number;
  resetAt?: string;
}
