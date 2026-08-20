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

export interface UserLearningProgress {
  nextStep: "ONBOARDING_REQUIRED" | "ASSESSMENT_REQUIRED" | "READY_FOR_PLAN";
  onboardingStep: OnboardingProgress["step"];
}

export type SelfRating = "BEGINNER" | "BASIC" | "INTERMEDIATE" | "UPPER_INTERMEDIATE" | "ADVANCED";

export interface SelfAssessmentRequest {
  listening: SelfRating;
  speaking: SelfRating;
  reading: SelfRating;
  writing: SelfRating;
}

export interface AssessmentSession {
  assessmentId: string;
  status: "IN_PROGRESS" | "PAUSED" | "PROCESSING" | "COMPLETED";
  targetMinutes: number;
  estimatedRemainingMinutes?: number | null;
}

export interface AssessmentItem {
  itemId: string;
  skill: string;
  type: "MULTIPLE_CHOICE" | "SHORT_TEXT" | "AUDIO_RESPONSE" | "REPEAT" | "RETELL";
  prompt: string;
  options: string[];
  timeLimitSeconds?: number | null;
}

export interface AssessmentAnswerRequest {
  itemId: string;
  answerType: "OPTION" | "TEXT" | "AUDIO";
  option?: string | null;
  text?: string | null;
  audioAssetId?: string | null;
  clientDurationMs?: number | null;
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

export interface AdminDashboardSummary {
  totalUsers: number;
  activeUsersToday: number;
  newUsersToday: number;
  aiRequestsToday: number;
  usersReachedQuotaLimit: number;
  activeDefaultProvider: string;
}

export interface AdminUserPage {
  items: AdminUserSummary[];
  page: number;
  size: number;
  total: number;
}

export interface AdminUserSummary {
  userId: number;
  userKey: string;
  email: string;
  status: "ACTIVE" | "DISABLED" | string;
  roles: string[];
  createdAt: string;
  lastLoginAt?: string | null;
}

export interface AdminUserDetail extends AdminUserSummary {
  locale: string;
  timezone: string;
  authVersion: number;
  authorities: string[];
  updatedAt: string;
  disabledAt?: string | null;
}

export interface AdminUserSearchRequest {
  q?: string;
  status?: "ACTIVE" | "DISABLED" | "";
  role?: string;
  page?: number;
  size?: number;
}

export interface AdminUserStatusRequest {
  status: "ACTIVE" | "DISABLED";
}

export interface AdminUserRolesRequest {
  roles: string[];
}

export interface AdminQuotaPolicyRequest {
  dailyLimitOverride?: number | null;
  unlimited: boolean;
}

export interface AdminQuotaBonusRequest {
  bonus: number;
}

export interface AdminQuotaState {
  userKey: string;
  dailyLimitOverride?: number | null;
  unlimited: boolean;
  quotaDate: string;
  dailyLimit: number;
  used: number;
  reserved: number;
  bonus: number;
  remaining: number;
}

export interface AdminSystemSetting {
  key: string;
  value: string;
  valueType: "STRING" | "INTEGER" | "BOOLEAN" | "JSON";
  description: string;
  updatedAt: string;
}

export interface AdminSystemSettingRequest {
  value: string;
  valueType: "STRING" | "INTEGER" | "BOOLEAN" | "JSON";
  description?: string;
}

export interface AdminAuditPage {
  items: AdminAuditEntry[];
  page: number;
  size: number;
  total: number;
}

export interface AdminAuditEntry {
  id: number;
  actorUserId?: number | null;
  actorEmail?: string | null;
  actionCode: string;
  targetType: string;
  targetKey: string;
  createdAt: string;
}

export interface AiProvider {
  providerCode: string;
  providerType: "OPENAI" | "OPENAI_COMPATIBLE" | "GEMINI";
  displayName: string;
  enabled: boolean;
  defaultLlm: boolean;
  defaultAsr: boolean;
  defaultTts: boolean;
  baseUrl: string;
  llmModel: string;
  asrModel: string | null;
  ttsModel: string | null;
  ttsVoice: string | null;
  timeoutSeconds: number;
  apiKeyConfigured: boolean;
  apiKeyMaskedHint?: string | null;
}

export interface AiProviderUpdateRequest {
  providerType: "OPENAI" | "OPENAI_COMPATIBLE" | "GEMINI";
  displayName: string;
  enabled: boolean;
  defaultLlm: boolean;
  defaultAsr: boolean;
  defaultTts: boolean;
  baseUrl: string;
  llmModel: string;
  asrModel: string | null;
  ttsModel: string | null;
  ttsVoice: string | null;
  timeoutSeconds?: number;
}

export interface AiProviderSecretRequest {
  apiKey: string;
}

export interface AiProviderConnectionTestResult {
  success: boolean;
  latencyMs: number;
  error?: string | null;
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

export type PrescriptionStatus = "ACTIVE" | "SUPERSEDED";
export type PrescriptionBlockType = "ACQUISITION" | "OUTPUT" | "REVIEW" | "TRANSFER";
export type PrescriptionTrainingType = "COMPREHENSION" | "GUIDED_SPEAKING" | "ROLE_PLAY" | "REVIEW" | "TRANSFER";
export type PrescriptionBlockStatus = "READY" | "SKIPPED";
export type PrescriptionRegenerationReason =
  | "TOO_HARD"
  | "TOO_EASY"
  | "TIME_INSUFFICIENT"
  | "TOPIC_REJECTED"
  | "TEMPORARY_GOAL";

export interface PrescriptionGoal {
  code: string;
  label: string;
}

export interface PrescriptionExperience {
  seasonId: string;
  episodeId: string;
  sceneId: string;
  title: string;
}

export interface PrescriptionResourceReference {
  resourceId: string;
  resourceVersion: string;
}

export interface PrescriptionTaskHero {
  assetId: string;
  url?: string | null;
  aspectRatio: string;
  focalPoint: { x: number; y: number };
  altText: string;
}

export interface PrescriptionBlock {
  blockId: string;
  sequence: number;
  type: PrescriptionBlockType;
  title: string;
  skillUnitVariantId: string;
  resource: PrescriptionResourceReference;
  episodeMappingId: string;
  difficulty: "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
  scaffolding: "HIGH" | "MEDIUM" | "LOW" | "NONE";
  trainingType: PrescriptionTrainingType;
  estimatedMinutes: number;
  expectedEvidence: string[];
  fallbackResource?: PrescriptionResourceReference | null;
  recommendationFactors: Record<string, number>;
  taskHero: PrescriptionTaskHero;
  status: PrescriptionBlockStatus;
}

export interface DailyLearningPrescription {
  prescriptionId: string;
  version: number;
  learningDate: string;
  timezone: string;
  status: PrescriptionStatus;
  priorityGoal: PrescriptionGoal;
  rationale: string;
  reasonCodes: string[];
  estimatedMinutes: number;
  experience: PrescriptionExperience;
  blocks: PrescriptionBlock[];
  generatedAt: string;
  expiresAt: string;
}

export interface PrescriptionRegenerationRequest {
  currentPrescriptionId: string;
  currentVersion: number;
  reason: PrescriptionRegenerationReason;
  availableMinutes?: number | null;
  temporaryGoal?: string | null;
  note?: string | null;
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
  fallbackAvailable?: boolean;
}
