import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { createApiClient, type AuthResponse, type QuotaStatus, type TrainingSessionCompletion } from "../shared/api";
import { CoachWorkspace } from "../features/coach/CoachWorkspace";
import { TodayCoachHome, type CoachSelection } from "../features/coach/TodayCoachHome";
import { SummaryView } from "../features/summary/SummaryView";
import { OnboardingPanel } from "../features/onboarding/OnboardingPanel";
import { DEFAULT_ONBOARDING_STATE, type LocalOnboardingState } from "../shared/session/localSession";
import {
  clearStoredAuthSession,
  loadStoredAuthSession,
  saveStoredAuthSession,
  toStoredAuthSession,
  type StoredAuthSession,
} from "../shared/auth/authStore";
import { I18nProvider, normalizeLocale, useI18n, type Locale } from "../shared/i18n";
import { loadPracticeHistory, savePracticeCompletion, type PracticeHistoryItem } from "../shared/history/practiceHistory";

type AppView = "today" | "history" | "account";
type QuotaLoadState = { status: "idle" | "loading" | "error"; quota: QuotaStatus | null };

export function App() {
  const [authSession, setAuthSession] = useState<StoredAuthSession | null>(() => loadStoredAuthSession());
  const initialLocale = authSession?.user.locale;

  return (
    <I18nProvider initialLocale={initialLocale}>
      <LearnerApp authSession={authSession} setAuthSession={setAuthSession} />
    </I18nProvider>
  );
}

function LearnerApp({
  authSession,
  setAuthSession,
}: {
  authSession: StoredAuthSession | null;
  setAuthSession: (session: StoredAuthSession | null) => void;
}) {
  const { t, locale, setLocale } = useI18n();
  const accessTokenRef = useRef(authSession?.accessToken);
  const [bootState, setBootState] = useState(authSession ? "ready" : "refreshing");
  const [onboardingState, setOnboardingState] = useState<LocalOnboardingState>(DEFAULT_ONBOARDING_STATE);
  const [view, setView] = useState<AppView>("today");
  const [coachSelection, setCoachSelection] = useState<CoachSelection | null>(null);
  const [completion, setCompletion] = useState<TrainingSessionCompletion | null>(null);
  const [quotaState, setQuotaState] = useState<QuotaLoadState>({ status: "idle", quota: null });
  const [practiceHistory, setPracticeHistory] = useState<PracticeHistoryItem[]>([]);

  const acceptAuthResponse = useCallback(
    (response: AuthResponse) => {
      const session = toStoredAuthSession(response);
      accessTokenRef.current = session.accessToken;
      saveStoredAuthSession(session);
      setAuthSession(session);
      setLocale(normalizeLocale(session.user.locale));
      return session;
    },
    [setAuthSession, setLocale],
  );

  const clearAuth = useCallback(() => {
    accessTokenRef.current = undefined;
    clearStoredAuthSession();
    setAuthSession(null);
    setOnboardingState(DEFAULT_ONBOARDING_STATE);
    setCoachSelection(null);
    setCompletion(null);
    setView("today");
    setQuotaState({ status: "idle", quota: null });
    setPracticeHistory([]);
  }, [setAuthSession]);

  const refreshAccessToken = useCallback(async () => {
    try {
      const response = await createApiClient().refreshSession();
      acceptAuthResponse(response);
      return true;
    } catch {
      clearAuth();
      return false;
    }
  }, [acceptAuthResponse, clearAuth]);

  const apiClient = useMemo(
    () =>
      createApiClient({
        accessTokenProvider: () => accessTokenRef.current,
        onUnauthorized: refreshAccessToken,
      }),
    [refreshAccessToken],
  );

  const loadQuota = useCallback(async () => {
    if (!accessTokenRef.current) {
      return;
    }
    setQuotaState((current) => ({ status: "loading", quota: current.quota }));
    try {
      const quota = await apiClient.getCurrentQuota();
      setQuotaState({ status: "idle", quota });
    } catch {
      setQuotaState((current) => ({ status: "error", quota: current.quota }));
    }
  }, [apiClient]);

  useEffect(() => {
    accessTokenRef.current = authSession?.accessToken;
  }, [authSession?.accessToken]);

  useEffect(() => {
    let cancelled = false;
    async function boot() {
      if (authSession) {
        setBootState("ready");
        return;
      }
      setBootState("refreshing");
      const refreshed = await refreshAccessToken();
      if (!cancelled) {
        setBootState(refreshed ? "ready" : "ready");
      }
    }
    void boot();
    return () => {
      cancelled = true;
    };
  }, [authSession, refreshAccessToken]);

  useEffect(() => {
    if (!authSession) {
      return;
    }
    const userEmail = authSession.user.email;
    let cancelled = false;
    async function loadAccountState() {
      setPracticeHistory(loadPracticeHistory(userEmail));
      try {
        const progress = await apiClient.getOnboardingProgress();
        if (!cancelled) {
          setOnboardingState({ ...DEFAULT_ONBOARDING_STATE, completed: progress.completed });
        }
      } catch {
        if (!cancelled) {
          setOnboardingState(DEFAULT_ONBOARDING_STATE);
        }
      }
      await loadQuota();
    }
    void loadAccountState();
    return () => {
      cancelled = true;
    };
  }, [apiClient, authSession, loadQuota]);

  async function handleLogout() {
    try {
      await apiClient.logout();
    } catch {
      // Local cleanup still matters if the refresh token has already expired.
    } finally {
      clearAuth();
    }
  }

  if (bootState === "refreshing") {
    return (
      <main className="app-shell">
        <section className="hero">
          <p className="eyebrow">English Tutor</p>
          <h1>{t("app.loading")}</h1>
        </section>
      </main>
    );
  }

  if (!authSession) {
    return <AuthScreen onAuthenticated={acceptAuthResponse} />;
  }

  if (!onboardingState.completed) {
    return (
      <OnboardingPanel
        apiClient={apiClient}
        initialState={onboardingState}
        onStateChange={setOnboardingState}
        onComplete={(completed) => {
          setOnboardingState(completed);
          void loadQuota();
        }}
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
          setView("today");
          void loadQuota();
        }}
      />
    );
  }

  if (coachSelection) {
    return (
      <CoachWorkspace
        apiClient={apiClient}
        quota={quotaState.quota}
        selection={coachSelection}
        onBack={() => setCoachSelection(null)}
        onQuotaChanged={loadQuota}
        onCompleted={(nextCompletion) => {
          setPracticeHistory(savePracticeCompletion(authSession.user.email, nextCompletion));
          setCompletion(nextCompletion);
          void loadQuota();
        }}
      />
    );
  }

  return (
    <main className="learner-shell">
      <header className="learner-topbar">
        <div>
          <strong>English Tutor</strong>
          <span>{authSession.user.email}</span>
        </div>
        <nav aria-label="Learner navigation">
          <button className={view === "today" ? "selected" : ""} type="button" onClick={() => setView("today")}>
            {t("app.nav.today")}
          </button>
          <button className={view === "history" ? "selected" : ""} type="button" onClick={() => setView("history")}>
            {t("app.nav.history")}
          </button>
          <button className={view === "account" ? "selected" : ""} type="button" onClick={() => setView("account")}>
            {t("app.nav.account")}
          </button>
          <select value={locale} onChange={(event) => setLocale(event.target.value as Locale)} aria-label={t("account.locale")}>
            <option value="zh-CN">Chinese</option>
            <option value="en">English</option>
          </select>
          <button type="button" onClick={handleLogout}>
            {t("app.nav.logout")}
          </button>
        </nav>
      </header>

      {view === "account" ? (
        <AccountPage user={authSession.user} quotaState={quotaState} onRefreshQuota={loadQuota} />
      ) : view === "history" ? (
        <HistoryPage history={practiceHistory} />
      ) : (
        <TodayCoachHome
          apiClient={apiClient}
          quota={quotaState.quota}
          quotaLoading={quotaState.status === "loading"}
          onRefreshQuota={loadQuota}
          onOpenAccount={() => setView("account")}
          onStart={setCoachSelection}
        />
      )}
    </main>
  );
}

function HistoryPage({ history }: { history: PracticeHistoryItem[] }) {
  const { t } = useI18n();
  return (
    <section className="history-page">
      <div className="section-head">
        <p className="eyebrow">{t("app.nav.history")}</p>
        <h1>{t("history.title")}</h1>
      </div>
      {history.length === 0 ? (
        <article className="settings-panel empty-history">
          <p className="panel-feedback">{t("history.empty")}</p>
        </article>
      ) : (
        <div className="history-list">
          {history.map((item) => (
            <article className="history-card" key={item.sessionId}>
              <div className="panel-header">
                <span>{new Date(item.completedAt).toLocaleString()}</span>
                <strong>{t("history.evidence", { evidence: item.evidenceCount })}</strong>
              </div>
              <ul>
                {[...item.highlights, ...item.memorableItems, ...item.nextFocus].slice(0, 5).map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (response: AuthResponse) => void }) {
  const { t, locale, setLocale } = useI18n();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitState, setSubmitState] = useState<"idle" | "submitting" | "error">("idle");
  const apiClient = useMemo(() => createApiClient(), []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitState("submitting");
    try {
      const request = { email: email.trim(), password };
      const response = mode === "login" ? await apiClient.login(request) : await apiClient.register(request);
      onAuthenticated(response);
      setSubmitState("idle");
    } catch {
      setSubmitState("error");
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-copy">
        <p className="eyebrow">English Tutor</p>
        <h1>{t("auth.title")}</h1>
        <p className="summary">{t("auth.subtitle")}</p>
      </section>
      <form className="auth-form" onSubmit={submit}>
        <div className="auth-tabs" role="tablist" aria-label="Authentication mode">
          <button className={mode === "login" ? "selected" : ""} type="button" onClick={() => setMode("login")}>
            {t("auth.loginTab")}
          </button>
          <button className={mode === "register" ? "selected" : ""} type="button" onClick={() => setMode("register")}>
            {t("auth.registerTab")}
          </button>
        </div>
        <label className="field-stack">
          <span>{t("auth.email")}</span>
          <input type="email" value={email} autoComplete="email" onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <label className="field-stack">
          <span>{t("auth.password")}</span>
          <input
            type="password"
            value={password}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            minLength={8}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        <select value={locale} onChange={(event) => setLocale(event.target.value as Locale)} aria-label={t("account.locale")}>
          <option value="zh-CN">Chinese</option>
          <option value="en">English</option>
        </select>
        {submitState === "error" ? <p className="form-error">{t("auth.error")}</p> : null}
        <button className="primary-action" type="submit" disabled={submitState === "submitting"}>
          {submitState === "submitting" ? t(mode === "login" ? "auth.submittingLogin" : "auth.submittingRegister") : t(mode === "login" ? "auth.login" : "auth.register")}
        </button>
        <button className="text-button" type="button" onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {t(mode === "login" ? "auth.switchToRegister" : "auth.switchToLogin")}
        </button>
      </form>
    </main>
  );
}

function AccountPage({
  user,
  quotaState,
  onRefreshQuota,
}: {
  user: StoredAuthSession["user"];
  quotaState: QuotaLoadState;
  onRefreshQuota: () => Promise<void>;
}) {
  const { t, locale, setLocale } = useI18n();
  const quota = quotaState.quota;

  return (
    <section className="account-page">
      <div className="section-head">
        <p className="eyebrow">{t("app.nav.account")}</p>
        <h1>{t("account.title")}</h1>
      </div>
      <div className="account-grid">
        <article className="settings-panel">
          <InfoRow label={t("account.email")} value={user.email} />
          <InfoRow label={t("account.status")} value={user.status} />
          <InfoRow label={t("account.roles")} value={user.roles.join(", ") || "USER"} />
          <label className="settings-row">
            <span>{t("account.locale")}</span>
            <select value={locale} onChange={(event) => setLocale(event.target.value as Locale)}>
              <option value="zh-CN">Chinese</option>
              <option value="en">English</option>
            </select>
          </label>
        </article>
        <article className="settings-panel quota-panel">
          <div className="panel-header stacked">
            <span>{t("account.quotaTitle")}</span>
            <strong>{quota ? quotaLabel(quota, t) : t("account.loadingQuota")}</strong>
          </div>
          {quota ? <QuotaMeter quota={quota} /> : <p className="panel-feedback">{t("account.quotaUnavailable")}</p>}
          <p className="panel-feedback">{t("account.quotaHelp")}</p>
          <button className="secondary-action" type="button" onClick={() => void onRefreshQuota()}>
            {t("account.refreshQuota")}
          </button>
        </article>
      </div>
    </section>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="settings-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function QuotaMeter({ quota }: { quota: QuotaStatus }) {
  const total = quota.unlimited ? Math.max(quota.used + quota.remaining, quota.dailyLimit + quota.bonus, 1) : quota.dailyLimit + quota.bonus;
  const usedPercent = quota.unlimited ? 0 : Math.min(100, Math.round((quota.used / Math.max(total, 1)) * 100));
  return (
    <div className="quota-meter" aria-label="Daily quota meter">
      <div style={{ width: `${usedPercent}%` }} />
    </div>
  );
}

function quotaLabel(quota: QuotaStatus, t: (key: string, params?: Record<string, string | number>) => string): string {
  if (quota.unlimited) {
    return t("home.quotaUnlimited");
  }
  return t("home.quotaRemaining", { remaining: quota.remaining });
}
