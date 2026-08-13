import { parseSseStream, type SseEventHandler } from "./sse";
import type {
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
  StartTrainingSessionRequest,
  TaskAttemptReceipt,
  TaskAttemptRequest,
  TrainingSession,
  TrainingSessionCompletion,
} from "./types";

const DEFAULT_BASE_URL = "http://localhost:8080";

export interface ApiClientOptions {
  baseUrl?: string;
  userKey?: string;
  fetchFn?: typeof fetch;
  idempotencyKeyFactory?: () => string;
}

export interface ApiClient {
  readonly baseUrl: string;
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
  userKey?: string;
  idempotencyKey?: string;
  signal?: AbortSignal;
}

interface JsonRequestOptions extends RequestOptions {
  method?: "GET" | "POST" | "PUT";
  body?: unknown;
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
    const response = await fetchFn(`${baseUrl}${path}`, {
      method,
      headers: buildHeaders(options.userKey, requestOptions, method, "application/json"),
      body: requestOptions.body === undefined ? undefined : JSON.stringify(requestOptions.body),
      signal: requestOptions.signal,
    });

    if (!response.ok) {
      throw await toApiError(response);
    }

    return (await response.json()) as T;
  }

  function mutationOptions(requestOptions: RequestOptions = {}): RequestOptions {
    return {
      ...requestOptions,
      idempotencyKey: requestOptions.idempotencyKey ?? idempotencyKeyFactory(),
    };
  }

  return {
    baseUrl,
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
      const response = await fetchFn(
        `${baseUrl}/api/v1/conversations/${encodeURIComponent(sessionId)}/messages/stream`,
        {
          method: "POST",
          headers: buildHeaders(options.userKey, optionsWithIdempotency, "POST", "application/json"),
          body: JSON.stringify(request),
          signal: requestOptions?.signal,
        },
      );

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

function buildHeaders(
  defaultUserKey: string | undefined,
  options: RequestOptions,
  method: string,
  contentType?: string,
): Headers {
  const headers = new Headers();
  const userKey = options.userKey ?? defaultUserKey;
  if (userKey) {
    headers.set("X-User-Key", userKey);
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
