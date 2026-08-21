import { parseSseStream, type SseEventHandler } from "./sse";
import type {
  AuthRequest,
  AuthResponse,
  AuthUser,
  AssessmentAnswerRequest,
  AssessmentItem,
  AssessmentSession,
  AdminAuditPage,
  AdminDashboardSummary,
  AdminQuotaBonusRequest,
  AdminQuotaPolicyRequest,
  AdminQuotaState,
  AdminSystemSetting,
  AdminSystemSettingRequest,
  AdminUserDetail,
  AdminUserPage,
  AdminUserRolesRequest,
  AdminUserSearchRequest,
  AdminUserStatusRequest,
  AiProvider,
  AiProviderConnectionTestResult,
  AiProviderSecretRequest,
  AiProviderUpdateRequest,
  AudioUploadRequest,
  AudioUploadResponse,
  ConversationMessageRequest,
  CurrentTrainingTask,
  DailyLearningPrescription,
  LearningPlan,
  LearningResourceDetail,
  LessonSession,
  LessonAttemptReceipt,
  LessonFeedbackCompletion,
  LessonStep,
  MediaAccessRequest,
  MediaAccessResponse,
  OnboardingProgress,
  SelfAssessmentRequest,
  UserLearningProgress,
  PlanAdjustmentRequest,
  PreferenceRequest,
  PrimaryGoal,
  PrivacySettings,
  PrescriptionRegenerationRequest,
  ProblemResponse,
  ProfileSummary,
  QuotaStatus,
  StartTrainingSessionRequest,
  StartLessonSessionRequest,
  SubmitLessonAttemptRequest,
  TaskAttemptReceipt,
  TaskAttemptRequest,
  TrainingSession,
  TrainingSessionCompletion,
  TranscriptConfirmationRequest,
  RolePlayMessageRequest,
  RolePlayTurnPage,
} from "./types";

const DEFAULT_BASE_URL = "http://localhost:8080";

export interface ApiClientOptions {
  baseUrl?: string;
  accessToken?: string;
  accessTokenProvider?: () => string | undefined;
  fetchFn?: typeof fetch;
  idempotencyKeyFactory?: () => string;
  onUnauthorized?: () => Promise<boolean>;
}

export interface ApiClient {
  readonly baseUrl: string;
  register(request: AuthRequest): Promise<AuthResponse>;
  login(request: AuthRequest): Promise<AuthResponse>;
  refreshSession(): Promise<AuthResponse>;
  logout(): Promise<void>;
  me(options?: RequestOptions): Promise<AuthUser>;
  getCurrentQuota(options?: RequestOptions): Promise<QuotaStatus>;
  getAdminDashboard(options?: RequestOptions): Promise<AdminDashboardSummary>;
  searchAdminUsers(request?: AdminUserSearchRequest, options?: RequestOptions): Promise<AdminUserPage>;
  getAdminUser(userKey: string, options?: RequestOptions): Promise<AdminUserDetail>;
  updateAdminUserStatus(userKey: string, request: AdminUserStatusRequest, options?: RequestOptions): Promise<AdminUserDetail>;
  replaceAdminUserRoles(userKey: string, request: AdminUserRolesRequest, options?: RequestOptions): Promise<AdminUserDetail>;
  updateAdminQuotaPolicy(userKey: string, request: AdminQuotaPolicyRequest, options?: RequestOptions): Promise<AdminQuotaState>;
  resetAdminUserQuotaToday(userKey: string, options?: RequestOptions): Promise<AdminQuotaState>;
  addAdminUserQuotaBonus(userKey: string, request: AdminQuotaBonusRequest, options?: RequestOptions): Promise<AdminQuotaState>;
  listAdminSystemSettings(options?: RequestOptions): Promise<AdminSystemSetting[]>;
  updateAdminSystemSetting(key: string, request: AdminSystemSettingRequest, options?: RequestOptions): Promise<AdminSystemSetting>;
  listAdminAudit(request?: { page?: number; size?: number }, options?: RequestOptions): Promise<AdminAuditPage>;
  listAiProviders(options?: RequestOptions): Promise<AiProvider[]>;
  getAiProvider(providerCode: string, options?: RequestOptions): Promise<AiProvider>;
  putAiProvider(providerCode: string, request: AiProviderUpdateRequest, options?: RequestOptions): Promise<AiProvider>;
  replaceAiProviderSecret(providerCode: string, request: AiProviderSecretRequest, options?: RequestOptions): Promise<AiProvider>;
  testAiProviderConnection(providerCode: string, options?: RequestOptions): Promise<AiProviderConnectionTestResult>;
  putPrimaryGoal(goal: PrimaryGoal, options?: RequestOptions): Promise<ProfileSummary>;
  putPreferences(request: PreferenceRequest, options?: RequestOptions): Promise<ProfileSummary>;
  getOnboardingProgress(options?: RequestOptions): Promise<OnboardingProgress>;
  getUserLearningProgress(options?: RequestOptions): Promise<UserLearningProgress>;
  submitSelfAssessment(request: SelfAssessmentRequest, options?: RequestOptions): Promise<unknown>;
  getCurrentAssessment(options?: RequestOptions): Promise<AssessmentSession | undefined>;
  startAssessment(options?: RequestOptions): Promise<AssessmentSession>;
  getNextAssessmentItem(assessmentId: string, options?: RequestOptions): Promise<AssessmentItem | undefined>;
  submitAssessmentAnswer(assessmentId: string, request: AssessmentAnswerRequest, options?: RequestOptions): Promise<unknown>;
  completeAssessment(assessmentId: string, options?: RequestOptions): Promise<unknown>;
  getTodayPlan(options?: RequestOptions): Promise<LearningPlan>;
  adjustTodayPlan(request: PlanAdjustmentRequest, options?: RequestOptions): Promise<LearningPlan>;
  getTodayPrescription(
    query?: { date?: string; timezone?: string },
    options?: RequestOptions,
  ): Promise<DailyLearningPrescription>;
  regenerateTodayPrescription(
    request: PrescriptionRegenerationRequest,
    options?: RequestOptions,
  ): Promise<DailyLearningPrescription>;
  startLessonSession(request: StartLessonSessionRequest, options?: RequestOptions): Promise<LessonSession>;
  getLessonSession(sessionId: string, options?: RequestOptions): Promise<LessonSession>;
  pauseLessonSession(sessionId: string, options?: RequestOptions): Promise<LessonSession>;
  resumeLessonSession(sessionId: string, options?: RequestOptions): Promise<LessonSession>;
  completeLessonStep(sessionId: string, stepId: LessonStep, options?: RequestOptions): Promise<LessonSession>;
  submitLessonAttempt(sessionId: string, request: SubmitLessonAttemptRequest, options?: RequestOptions): Promise<LessonAttemptReceipt>;
  uploadAudio(request: AudioUploadRequest, options?: RequestOptions): Promise<AudioUploadResponse>;
  confirmLessonAttemptTranscript(
    sessionId: string,
    attemptId: string,
    request: TranscriptConfirmationRequest,
    options?: RequestOptions,
  ): Promise<LessonAttemptReceipt>;
  getLessonAttempt(sessionId: string, attemptId: string, options?: RequestOptions): Promise<LessonAttemptReceipt>;
  completeLessonFeedback(sessionId: string, attemptId: string, options?: RequestOptions): Promise<LessonFeedbackCompletion>;
  listRolePlayTurns(sessionId: string, options?: RequestOptions): Promise<RolePlayTurnPage>;
  streamRolePlayMessage(
    sessionId: string,
    request: RolePlayMessageRequest,
    onEvent: SseEventHandler,
    options?: RequestOptions,
  ): Promise<void>;
  getLearningResourceVersion(resourceId: string, version: string, options?: RequestOptions): Promise<LearningResourceDetail>;
  createLearningResourceMediaAccess(
    resourceId: string,
    request: MediaAccessRequest,
    options?: RequestOptions,
  ): Promise<MediaAccessResponse>;
  startTrainingSession(request: StartTrainingSessionRequest, options?: RequestOptions): Promise<TrainingSession>;
  getTrainingSession(sessionId: string, options?: RequestOptions): Promise<TrainingSession>;
  pauseTrainingSession(sessionId: string, options?: RequestOptions): Promise<TrainingSession>;
  resumeTrainingSession(sessionId: string, options?: RequestOptions): Promise<TrainingSession>;
  completeTrainingSession(sessionId: string, options?: RequestOptions): Promise<TrainingSessionCompletion>;
  getCurrentTrainingTask(sessionId: string, options?: RequestOptions): Promise<CurrentTrainingTask>;
  submitTaskAttempt(
    sessionId: string,
    taskId: string,
    request: TaskAttemptRequest,
    options?: RequestOptions,
  ): Promise<TaskAttemptReceipt>;
  getPrivacySettings(options?: RequestOptions): Promise<PrivacySettings>;
  putPrivacySettings(request: PrivacySettings, options?: RequestOptions): Promise<PrivacySettings>;
  streamConversationMessage(
    sessionId: string,
    request: ConversationMessageRequest,
    onEvent: SseEventHandler,
    options?: RequestOptions,
  ): Promise<void>;
}

interface RequestOptions {
  idempotencyKey?: string;
  signal?: AbortSignal;
}

interface JsonRequestOptions extends RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH";
  body?: unknown;
  retryOnUnauthorized?: boolean;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly problem?: ProblemResponse,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function createApiClient(options: ApiClientOptions = {}): ApiClient {
  const baseUrl = normalizeBaseUrl(resolveBaseUrl(options.baseUrl));
  const fetchFn = options.fetchFn ?? fetch;
  const idempotencyKeyFactory = options.idempotencyKeyFactory ?? createIdempotencyKey;

  async function requestJson<T>(path: string, requestOptions: JsonRequestOptions = {}): Promise<T> {
    const method = requestOptions.method ?? "GET";
    let response = await fetchJson(path, method, requestOptions);

    if (response.status === 401 && requestOptions.retryOnUnauthorized !== false && options.onUnauthorized) {
      const refreshed = await options.onUnauthorized();
      if (refreshed) {
        response = await fetchJson(path, method, { ...requestOptions, retryOnUnauthorized: false });
      }
    }

    if (!response.ok) {
      throw await toApiError(response);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  async function requestForm<T>(path: string, form: FormData, requestOptions: RequestOptions = {}): Promise<T> {
    const resolved = mutationOptions(requestOptions);
    const send = () => fetchFn(`${baseUrl}${path}`, {
      method: "POST",
      headers: buildHeaders(options, resolved, "POST"),
      body: form,
      signal: resolved.signal,
      credentials: "include",
    });
    let response = await send();
    if (response.status === 401 && options.onUnauthorized && await options.onUnauthorized()) response = await send();
    if (!response.ok) throw await toApiError(response);
    return JSON.parse(await response.text()) as T;
  }

  function fetchJson(path: string, method: string, requestOptions: JsonRequestOptions): Promise<Response> {
    return fetchFn(`${baseUrl}${path}`, {
      method,
      headers: buildHeaders(options, requestOptions, method, "application/json"),
      body: requestOptions.body === undefined ? undefined : JSON.stringify(requestOptions.body),
      signal: requestOptions.signal,
      credentials: "include",
    });
  }

  function mutationOptions(requestOptions: RequestOptions = {}): RequestOptions {
    return {
      ...requestOptions,
      idempotencyKey: requestOptions.idempotencyKey ?? idempotencyKeyFactory(),
    };
  }

  return {
    baseUrl,
    register(request) {
      return requestJson<AuthResponse>("/api/v1/auth/register", {
        method: "POST",
        body: request,
        retryOnUnauthorized: false,
      });
    },
    login(request) {
      return requestJson<AuthResponse>("/api/v1/auth/login", {
        method: "POST",
        body: request,
        retryOnUnauthorized: false,
      });
    },
    refreshSession() {
      return requestJson<AuthResponse>("/api/v1/auth/refresh", {
        method: "POST",
        retryOnUnauthorized: false,
      });
    },
    logout() {
      return requestJson<void>("/api/v1/auth/logout", {
        method: "POST",
        retryOnUnauthorized: false,
      });
    },
    me(requestOptions) {
      return requestJson<AuthUser>("/api/v1/me", requestOptions);
    },
    getCurrentQuota(requestOptions) {
      return requestJson<QuotaStatus>("/api/v1/me/quota", requestOptions);
    },
    getAdminDashboard(requestOptions) {
      return requestJson<AdminDashboardSummary>("/api/v1/admin/dashboard", requestOptions);
    },
    searchAdminUsers(request = {}, requestOptions) {
      return requestJson<AdminUserPage>(`/api/v1/admin/users${toQueryString(request)}`, requestOptions);
    },
    getAdminUser(userKey, requestOptions) {
      return requestJson<AdminUserDetail>(`/api/v1/admin/users/${encodeURIComponent(userKey)}`, requestOptions);
    },
    updateAdminUserStatus(userKey, request, requestOptions) {
      return requestJson<AdminUserDetail>(`/api/v1/admin/users/${encodeURIComponent(userKey)}/status`, {
        ...mutationOptions(requestOptions),
        method: "PATCH",
        body: request,
      });
    },
    replaceAdminUserRoles(userKey, request, requestOptions) {
      return requestJson<AdminUserDetail>(`/api/v1/admin/users/${encodeURIComponent(userKey)}/roles`, {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    updateAdminQuotaPolicy(userKey, request, requestOptions) {
      return requestJson<AdminQuotaState>(`/api/v1/admin/users/${encodeURIComponent(userKey)}/quota-policy`, {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    resetAdminUserQuotaToday(userKey, requestOptions) {
      return requestJson<AdminQuotaState>(`/api/v1/admin/users/${encodeURIComponent(userKey)}/quota/reset-today`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    addAdminUserQuotaBonus(userKey, request, requestOptions) {
      return requestJson<AdminQuotaState>(`/api/v1/admin/users/${encodeURIComponent(userKey)}/quota/bonus`, {
        ...mutationOptions(requestOptions),
        method: "POST",
        body: request,
      });
    },
    listAdminSystemSettings(requestOptions) {
      return requestJson<AdminSystemSetting[]>("/api/v1/admin/settings", requestOptions);
    },
    updateAdminSystemSetting(key, request, requestOptions) {
      return requestJson<AdminSystemSetting>(`/api/v1/admin/settings/${encodeURIComponent(key)}`, {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    listAdminAudit(request = {}, requestOptions) {
      return requestJson<AdminAuditPage>(`/api/v1/admin/audit${toQueryString(request)}`, requestOptions);
    },
    listAiProviders(requestOptions) {
      return requestJson<AiProvider[]>("/api/v1/admin/ai-providers", requestOptions);
    },
    getAiProvider(providerCode, requestOptions) {
      return requestJson<AiProvider>(`/api/v1/admin/ai-providers/${encodeURIComponent(providerCode)}`, requestOptions);
    },
    putAiProvider(providerCode, request, requestOptions) {
      return requestJson<AiProvider>(`/api/v1/admin/ai-providers/${encodeURIComponent(providerCode)}`, {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    replaceAiProviderSecret(providerCode, request, requestOptions) {
      return requestJson<AiProvider>(`/api/v1/admin/ai-providers/${encodeURIComponent(providerCode)}/secret`, {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    testAiProviderConnection(providerCode, requestOptions) {
      return requestJson<AiProviderConnectionTestResult>(`/api/v1/admin/ai-providers/${encodeURIComponent(providerCode)}/test`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    putPrimaryGoal(goal, requestOptions) {
      return requestJson<ProfileSummary>("/api/v1/profile/primary-goal", {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: { goal },
      });
    },
    putPreferences(request, requestOptions) {
      return requestJson<ProfileSummary>("/api/v1/profile/preferences", {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    getOnboardingProgress(requestOptions) {
      return requestJson<OnboardingProgress>("/api/v1/onboarding/progress", requestOptions);
    },
    getUserLearningProgress(requestOptions) {
      return requestJson<UserLearningProgress>("/api/v1/users/me/progress", requestOptions);
    },
    submitSelfAssessment(request, requestOptions) {
      return requestJson("/api/v1/assessments/self", { ...mutationOptions(requestOptions), method: "POST", body: request });
    },
    getCurrentAssessment(requestOptions) {
      return requestJson<AssessmentSession>("/api/v1/assessments/current", requestOptions);
    },
    startAssessment(requestOptions) {
      return requestJson<AssessmentSession>("/api/v1/assessments", { ...mutationOptions(requestOptions), method: "POST", body: {} });
    },
    getNextAssessmentItem(assessmentId, requestOptions) {
      return requestJson<AssessmentItem>(`/api/v1/assessments/${encodeURIComponent(assessmentId)}/next`, requestOptions);
    },
    submitAssessmentAnswer(assessmentId, request, requestOptions) {
      return requestJson(`/api/v1/assessments/${encodeURIComponent(assessmentId)}/answers`, {
        ...mutationOptions(requestOptions), method: "POST", body: request,
      });
    },
    completeAssessment(assessmentId, requestOptions) {
      return requestJson(`/api/v1/assessments/${encodeURIComponent(assessmentId)}/complete`, {
        ...mutationOptions(requestOptions), method: "POST",
      });
    },
    getTodayPlan(requestOptions) {
      return requestJson<LearningPlan>("/api/v1/plans/today", requestOptions);
    },
    adjustTodayPlan(request, requestOptions) {
      return requestJson<LearningPlan>("/api/v1/plans/today/adjustments", {
        ...mutationOptions(requestOptions),
        method: "POST",
        body: request,
      });
    },
    getTodayPrescription(query = {}, requestOptions) {
      const params = new URLSearchParams();
      if (query.date) params.set("date", query.date);
      if (query.timezone) params.set("timezone", query.timezone);
      const suffix = params.size > 0 ? `?${params.toString()}` : "";
      return requestJson<DailyLearningPrescription>(`/api/v1/prescriptions/today${suffix}`, requestOptions);
    },
    regenerateTodayPrescription(request, requestOptions) {
      return requestJson<DailyLearningPrescription>("/api/v1/prescriptions/today/regenerations", {
        ...mutationOptions(requestOptions),
        method: "POST",
        body: request,
      });
    },
    startLessonSession(request, requestOptions) {
      return requestJson<LessonSession>("/api/v1/lesson-sessions", {
        ...mutationOptions(requestOptions),
        method: "POST",
        body: request,
      });
    },
    getLessonSession(sessionId, requestOptions) {
      return requestJson<LessonSession>(`/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}`, requestOptions);
    },
    pauseLessonSession(sessionId, requestOptions) {
      return requestJson<LessonSession>(`/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/pause`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    resumeLessonSession(sessionId, requestOptions) {
      return requestJson<LessonSession>(`/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/resume`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    completeLessonStep(sessionId, stepId, requestOptions) {
      return requestJson<LessonSession>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/steps/${encodeURIComponent(stepId)}/completions`,
        { ...mutationOptions(requestOptions), method: "POST" },
      );
    },
    submitLessonAttempt(sessionId, request, requestOptions) {
      return requestJson<LessonAttemptReceipt>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/attempts`,
        { ...mutationOptions(requestOptions), method: "POST", body: request },
      );
    },
    uploadAudio(request, requestOptions) {
      const form = new FormData();
      form.append("file", request.file, `recording.${request.file.type.includes("ogg") ? "ogg" : "webm"}`);
      form.append("durationMs", String(request.durationMs));
      form.append("purpose", request.purpose ?? "LESSON_ATTEMPT");
      if (request.sha256) form.append("sha256", request.sha256);
      return requestForm<AudioUploadResponse>("/api/v1/audio/uploads", form, requestOptions);
    },
    confirmLessonAttemptTranscript(sessionId, attemptId, request, requestOptions) {
      return requestJson<LessonAttemptReceipt>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/attempts/${encodeURIComponent(attemptId)}/transcript-confirmations`,
        { ...mutationOptions(requestOptions), method: "POST", body: request },
      );
    },
    getLessonAttempt(sessionId, attemptId, requestOptions) {
      return requestJson<LessonAttemptReceipt>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/attempts/${encodeURIComponent(attemptId)}`,
        requestOptions,
      );
    },
    completeLessonFeedback(sessionId, attemptId, requestOptions) {
      return requestJson<LessonFeedbackCompletion>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/attempts/${encodeURIComponent(attemptId)}/feedback-completions`,
        { ...mutationOptions(requestOptions), method: "POST" },
      );
    },
    getLearningResourceVersion(resourceId, version, requestOptions) {
      return requestJson<LearningResourceDetail>(
        `/api/v1/learning-resources/${encodeURIComponent(resourceId)}/versions/${encodeURIComponent(version)}`,
        requestOptions,
      );
    },
    listRolePlayTurns(sessionId, requestOptions) {
      return requestJson<RolePlayTurnPage>(
        `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/role-play/turns`, requestOptions,
      );
    },
    async streamRolePlayMessage(sessionId, request, onEvent, requestOptions) {
      const optionsWithIdempotency = mutationOptions(requestOptions);
      const path = `/api/v1/lesson-sessions/${encodeURIComponent(sessionId)}/role-play/messages/stream`;
      let response = await fetchFn(`${baseUrl}${path}`, {
        method: "POST",
        headers: buildHeaders(options, optionsWithIdempotency, "POST", "application/json"),
        body: JSON.stringify(request),
        signal: requestOptions?.signal,
        credentials: "include",
      });
      if (response.status === 401 && options.onUnauthorized && await options.onUnauthorized()) {
        response = await fetchFn(`${baseUrl}${path}`, {
          method: "POST",
          headers: buildHeaders(options, optionsWithIdempotency, "POST", "application/json"),
          body: JSON.stringify(request), signal: requestOptions?.signal, credentials: "include",
        });
      }
      if (!response.ok) throw await toApiError(response);
      if (!response.body) throw new ApiError("SSE response body is empty", response.status);
      await parseSseStream(response.body, onEvent);
    },
    createLearningResourceMediaAccess(resourceId, request, requestOptions) {
      return requestJson<MediaAccessResponse>(
        `/api/v1/learning-resources/${encodeURIComponent(resourceId)}/media-access`,
        { ...mutationOptions(requestOptions), method: "POST", body: request },
      );
    },
    startTrainingSession(request, requestOptions) {
      return requestJson<TrainingSession>("/api/v1/training-sessions", {
        ...mutationOptions(requestOptions),
        method: "POST",
        body: request,
      });
    },
    getTrainingSession(sessionId, requestOptions) {
      return requestJson<TrainingSession>(`/api/v1/training-sessions/${encodeURIComponent(sessionId)}`, requestOptions);
    },
    pauseTrainingSession(sessionId, requestOptions) {
      return requestJson<TrainingSession>(`/api/v1/training-sessions/${encodeURIComponent(sessionId)}/pause`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    resumeTrainingSession(sessionId, requestOptions) {
      return requestJson<TrainingSession>(`/api/v1/training-sessions/${encodeURIComponent(sessionId)}/resume`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    completeTrainingSession(sessionId, requestOptions) {
      return requestJson<TrainingSessionCompletion>(`/api/v1/training-sessions/${encodeURIComponent(sessionId)}/complete`, {
        ...mutationOptions(requestOptions),
        method: "POST",
      });
    },
    getCurrentTrainingTask(sessionId, requestOptions) {
      return requestJson<CurrentTrainingTask>(
        `/api/v1/training-sessions/${encodeURIComponent(sessionId)}/current-task`,
        requestOptions,
      );
    },
    submitTaskAttempt(sessionId, taskId, request, requestOptions) {
      return requestJson<TaskAttemptReceipt>(
        `/api/v1/training-sessions/${encodeURIComponent(sessionId)}/tasks/${encodeURIComponent(taskId)}/attempts`,
        {
          ...mutationOptions(requestOptions),
          method: "POST",
          body: request,
        },
      );
    },
    getPrivacySettings(requestOptions) {
      return requestJson<PrivacySettings>("/api/v1/settings/privacy", requestOptions);
    },
    putPrivacySettings(request, requestOptions) {
      return requestJson<PrivacySettings>("/api/v1/settings/privacy", {
        ...mutationOptions(requestOptions),
        method: "PUT",
        body: request,
      });
    },
    async streamConversationMessage(sessionId, request, onEvent, requestOptions) {
      const optionsWithIdempotency = mutationOptions(requestOptions);
      const path = `/api/v1/conversations/${encodeURIComponent(sessionId)}/messages/stream`;
      let response = await fetchFn(`${baseUrl}${path}`, {
        method: "POST",
        headers: buildHeaders(options, optionsWithIdempotency, "POST", "application/json"),
        body: JSON.stringify(request),
        signal: requestOptions?.signal,
        credentials: "include",
      });

      if (response.status === 401 && options.onUnauthorized) {
        const refreshed = await options.onUnauthorized();
        if (refreshed) {
          response = await fetchFn(`${baseUrl}${path}`, {
            method: "POST",
            headers: buildHeaders(options, optionsWithIdempotency, "POST", "application/json"),
            body: JSON.stringify(request),
            signal: requestOptions?.signal,
            credentials: "include",
          });
        }
      }

      if (!response.ok) {
        throw await toApiError(response);
      }
      if (!response.body) {
        throw new ApiError("SSE response body is empty", response.status);
      }

      await parseSseStream(response.body, onEvent);
    },
  };
}

function resolveBaseUrl(baseUrl?: string): string {
  return baseUrl ?? import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL;
}

function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.replace(/\/+$/, "");
}

function toQueryString(params: object): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if ((typeof value === "string" && value !== "") || typeof value === "number") {
      query.set(key, String(value));
    }
  }
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
}

function buildHeaders(
  clientOptions: ApiClientOptions,
  options: RequestOptions,
  method: string,
  contentType?: string,
): Headers {
  const headers = new Headers();
  const accessToken = clientOptions.accessTokenProvider?.() ?? clientOptions.accessToken;
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  if (method !== "GET" && options.idempotencyKey) {
    headers.set("Idempotency-Key", options.idempotencyKey);
  }
  if (contentType) {
    headers.set("Content-Type", contentType);
  }
  return headers;
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemResponse | undefined;
  try {
    problem = (await response.json()) as ProblemResponse;
  } catch {
    problem = undefined;
  }
  return new ApiError(problem?.detail ?? problem?.title ?? `Request failed with HTTP ${response.status}`, response.status, problem);
}

function createIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
