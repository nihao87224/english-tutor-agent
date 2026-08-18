import { parseSseStream, type SseEventHandler } from "./sse";
import type {
  AuthRequest,
  AuthResponse,
  AuthUser,
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
  AiProviderSecretRequest,
  AiProviderUpdateRequest,
  ConversationMessageRequest,
  CurrentTrainingTask,
  LearningPlan,
  OnboardingProgress,
  PlanAdjustmentRequest,
  PreferenceRequest,
  PrimaryGoal,
  PrivacySettings,
  ProblemResponse,
  ProfileSummary,
  QuotaStatus,
  StartTrainingSessionRequest,
  TaskAttemptReceipt,
  TaskAttemptRequest,
  TrainingSession,
  TrainingSessionCompletion,
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
  putPrimaryGoal(goal: PrimaryGoal, options?: RequestOptions): Promise<ProfileSummary>;
  putPreferences(request: PreferenceRequest, options?: RequestOptions): Promise<ProfileSummary>;
  getOnboardingProgress(options?: RequestOptions): Promise<OnboardingProgress>;
  getTodayPlan(options?: RequestOptions): Promise<LearningPlan>;
  adjustTodayPlan(request: PlanAdjustmentRequest, options?: RequestOptions): Promise<LearningPlan>;
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
