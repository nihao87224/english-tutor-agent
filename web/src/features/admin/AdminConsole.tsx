import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import type {
  AdminAuditPage,
  AdminDashboardSummary,
  AdminQuotaState,
  AdminSystemSetting,
  AdminUserDetail,
  AdminUserPage,
  AiProvider,
  AiProviderUpdateRequest,
  ApiClient,
  AuthUser,
} from "../../shared/api";
import { useI18n, type Locale } from "../../shared/i18n";

type AdminView = "overview" | "users" | "providers" | "settings" | "audit";
type LoadState<T> = { status: "loading" | "content" | "empty" | "error"; data: T | null };

const ADMIN_VIEWS: Array<{ view: AdminView; labelKey: string; path: string }> = [
  { view: "overview", labelKey: "admin.nav.overview", path: "/admin" },
  { view: "users", labelKey: "admin.nav.users", path: "/admin/users" },
  { view: "providers", labelKey: "admin.nav.providers", path: "/admin/providers" },
  { view: "settings", labelKey: "admin.nav.settings", path: "/admin/settings" },
  { view: "audit", labelKey: "admin.nav.audit", path: "/admin/audit" },
];

export function hasAdminRole(user: Pick<AuthUser, "roles">): boolean {
  return user.roles.some((role) => role.toUpperCase() === "ADMIN");
}

export function adminViewFromPath(pathname: string): AdminView {
  if (pathname.startsWith("/admin/users")) {
    return "users";
  }
  if (pathname.startsWith("/admin/providers")) {
    return "providers";
  }
  if (pathname.startsWith("/admin/settings")) {
    return "settings";
  }
  if (pathname.startsWith("/admin/audit")) {
    return "audit";
  }
  return "overview";
}

export function adminPathForView(view: AdminView): string {
  return ADMIN_VIEWS.find((item) => item.view === view)?.path ?? "/admin";
}

export function quotaUsagePercent(quota: AdminQuotaState): number {
  if (quota.unlimited) {
    return 0;
  }
  const total = quota.dailyLimit + quota.bonus;
  return Math.min(100, Math.round((quota.used / Math.max(total, 1)) * 100));
}

export function providerToUpdateRequest(provider: AiProvider): AiProviderUpdateRequest {
  return {
    providerType: provider.providerType,
    displayName: provider.displayName,
    enabled: provider.enabled,
    defaultLlm: provider.defaultLlm,
    defaultAsr: provider.defaultAsr,
    defaultTts: provider.defaultTts,
    baseUrl: provider.baseUrl,
    llmModel: provider.llmModel,
    asrModel: provider.asrModel,
    ttsModel: provider.ttsModel,
    ttsVoice: provider.ttsVoice,
    timeoutSeconds: provider.timeoutSeconds,
  };
}

export function AdminForbidden({ onOpenLearner }: { onOpenLearner: () => void }) {
  const { t } = useI18n();
  return (
    <main className="admin-forbidden">
      <section>
        <p className="eyebrow">403</p>
        <h1>{t("admin.forbidden.title")}</h1>
        <p className="summary">{t("admin.forbidden.body")}</p>
        <button className="primary-action" type="button" onClick={onOpenLearner}>
          {t("admin.openLearner")}
        </button>
      </section>
    </main>
  );
}

export function AdminConsole({
  apiClient,
  user,
  onLogout,
  onOpenLearner,
}: {
  apiClient: ApiClient;
  user: AuthUser;
  onLogout: () => void | Promise<void>;
  onOpenLearner: () => void;
}) {
  const { t, locale, setLocale } = useI18n();
  const [view, setView] = useState<AdminView>(() => adminViewFromPath(window.location.pathname));

  useEffect(() => {
    function onPopState() {
      setView(adminViewFromPath(window.location.pathname));
    }
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(nextView: AdminView) {
    setView(nextView);
    window.history.pushState(null, "", adminPathForView(nextView));
  }

  return (
    <main className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <strong>English Tutor</strong>
          <span>{t("admin.console")}</span>
        </div>
        <nav aria-label={t("admin.nav.label")}>
          {ADMIN_VIEWS.map((item) => (
            <button className={view === item.view ? "selected" : ""} type="button" key={item.view} onClick={() => navigate(item.view)}>
              <span>{item.view.slice(0, 2).toUpperCase()}</span>
              {t(item.labelKey)}
            </button>
          ))}
        </nav>
        <button className="admin-side-link" type="button" onClick={onOpenLearner}>
          {t("admin.openLearner")}
        </button>
      </aside>
      <section className="admin-main">
        <header className="admin-topbar">
          <div>
            <strong>{t(ADMIN_VIEWS.find((item) => item.view === view)?.labelKey ?? "admin.nav.overview")}</strong>
            <span>{user.email}</span>
          </div>
          <div className="admin-topbar-actions">
            <select value={locale} onChange={(event) => setLocale(event.target.value as Locale)} aria-label={t("account.locale")}>
              <option value="zh-CN">Chinese</option>
              <option value="en">English</option>
            </select>
            <button type="button" onClick={() => void onLogout()}>
              {t("app.nav.logout")}
            </button>
          </div>
        </header>
        {view === "users" ? (
          <UsersPage apiClient={apiClient} />
        ) : view === "providers" ? (
          <ProvidersPage apiClient={apiClient} />
        ) : view === "settings" ? (
          <SettingsPage apiClient={apiClient} />
        ) : view === "audit" ? (
          <AuditPage apiClient={apiClient} />
        ) : (
          <OverviewPage apiClient={apiClient} onNavigate={navigate} />
        )}
      </section>
    </main>
  );
}

function OverviewPage({ apiClient, onNavigate }: { apiClient: ApiClient; onNavigate: (view: AdminView) => void }) {
  const { t } = useI18n();
  const [state, setState] = useState<LoadState<AdminDashboardSummary>>({ status: "loading", data: null });

  const load = useCallback(async () => {
    setState({ status: "loading", data: null });
    try {
      const dashboard = await apiClient.getAdminDashboard();
      setState({ status: "content", data: dashboard });
    } catch {
      setState({ status: "error", data: null });
    }
  }, [apiClient]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section className="admin-page">
      <AdminHero eyebrow={t("admin.overview.eyebrow")} title={t("admin.overview.title")} body={t("admin.overview.body")} />
      {state.status === "loading" ? <StatusPanel text={t("admin.loading")} /> : null}
      {state.status === "error" ? <StatusPanel text={t("admin.error")} actionLabel={t("home.retry")} onAction={load} /> : null}
      {state.data ? (
        <>
          <div className="admin-metrics">
            <Metric label={t("admin.metric.totalUsers")} value={state.data.totalUsers} />
            <Metric label={t("admin.metric.activeToday")} value={state.data.activeUsersToday} />
            <Metric label={t("admin.metric.newToday")} value={state.data.newUsersToday} />
            <Metric label={t("admin.metric.aiRequests")} value={state.data.aiRequestsToday} />
            <Metric label={t("admin.metric.quotaLimited")} value={state.data.usersReachedQuotaLimit} />
            <Metric label={t("admin.metric.defaultProvider")} value={state.data.activeDefaultProvider || "-"} />
          </div>
          <div className="admin-quick-grid">
            <QuickAction title={t("admin.nav.users")} body={t("admin.quick.users")} onClick={() => onNavigate("users")} />
            <QuickAction title={t("admin.nav.providers")} body={t("admin.quick.providers")} onClick={() => onNavigate("providers")} />
            <QuickAction title={t("admin.nav.audit")} body={t("admin.quick.audit")} onClick={() => onNavigate("audit")} />
          </div>
        </>
      ) : null}
    </section>
  );
}

function UsersPage({ apiClient }: { apiClient: ApiClient }) {
  const { t } = useI18n();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<"ACTIVE" | "DISABLED" | "">("");
  const [role, setRole] = useState("");
  const [state, setState] = useState<LoadState<AdminUserPage>>({ status: "loading", data: null });
  const [selected, setSelected] = useState<AdminUserDetail | null>(null);
  const [quota, setQuota] = useState<AdminQuotaState | null>(null);
  const [dailyLimitOverride, setDailyLimitOverride] = useState("");
  const [unlimited, setUnlimited] = useState(false);
  const [bonus, setBonus] = useState("20");
  const [rolesDraft, setRolesDraft] = useState("");
  const [feedback, setFeedback] = useState("");
  const [operationError, setOperationError] = useState("");

  async function runOperation(operation: () => Promise<void>) {
    setOperationError("");
    try {
      await operation();
    } catch {
      setOperationError(t("admin.error"));
    }
  }

  const loadUsers = useCallback(async () => {
    setState((current) => ({ status: "loading", data: current.data }));
    try {
      const users = await apiClient.searchAdminUsers({ q: query, status, role, page: 0, size: 20 });
      setState({ status: users.items.length ? "content" : "empty", data: users });
    } catch {
      setState((current) => ({ status: "error", data: current.data }));
    }
  }, [apiClient, query, role, status]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  function openUser(userKey: string) {
    return runOperation(async () => {
      setFeedback("");
      setQuota(null);
      const detail = await apiClient.getAdminUser(userKey);
      setSelected(detail);
      setDailyLimitOverride("");
      setUnlimited(false);
      setRolesDraft(detail.roles.join(", "));
    });
  }

  function updateStatus(nextStatus: "ACTIVE" | "DISABLED") {
    if (!selected) {
      return;
    }
    return runOperation(async () => {
      const detail = await apiClient.updateAdminUserStatus(selected.userKey, { status: nextStatus });
      setSelected(detail);
      setFeedback(t("admin.users.statusSaved"));
      await loadUsers();
    });
  }

  function saveRoles() {
    if (!selected) {
      return;
    }
    return runOperation(async () => {
      const roles = rolesDraft
        .split(",")
        .map((item) => item.trim().toUpperCase())
        .filter(Boolean);
      const detail = await apiClient.replaceAdminUserRoles(selected.userKey, { roles });
      setSelected(detail);
      setRolesDraft(detail.roles.join(", "));
      setFeedback(t("admin.users.rolesSaved"));
      await loadUsers();
    });
  }

  function saveQuotaPolicy(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) {
      return;
    }
    return runOperation(async () => {
      const nextQuota = await apiClient.updateAdminQuotaPolicy(selected.userKey, {
        dailyLimitOverride: dailyLimitOverride === "" ? null : Number(dailyLimitOverride),
        unlimited,
      });
      setQuota(nextQuota);
      setFeedback(t("admin.users.quotaSaved"));
    });
  }

  function resetQuota() {
    if (!selected) {
      return;
    }
    return runOperation(async () => {
      setQuota(await apiClient.resetAdminUserQuotaToday(selected.userKey));
      setFeedback(t("admin.users.quotaReset"));
    });
  }

  function addBonus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) {
      return;
    }
    return runOperation(async () => {
      setQuota(await apiClient.addAdminUserQuotaBonus(selected.userKey, { bonus: Number(bonus) }));
      setFeedback(t("admin.users.bonusAdded"));
    });
  }

  return (
    <section className="admin-page">
      <AdminHero eyebrow={t("admin.users.eyebrow")} title={t("admin.users.title")} body={t("admin.users.body")} />
      <form className="admin-filter-bar" onSubmit={(event) => { event.preventDefault(); void loadUsers(); }}>
        <input value={query} placeholder={t("admin.users.search")} onChange={(event) => setQuery(event.target.value)} />
        <select value={status} onChange={(event) => setStatus(event.target.value as "ACTIVE" | "DISABLED" | "")}>
          <option value="">{t("admin.users.anyStatus")}</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
        <input value={role} placeholder={t("admin.users.role")} onChange={(event) => setRole(event.target.value)} />
        <button type="submit">{t("home.refresh")}</button>
      </form>
      {state.status === "loading" ? <StatusPanel text={t("admin.loading")} /> : null}
      {state.status === "error" ? <StatusPanel text={t("admin.error")} actionLabel={t("home.retry")} onAction={loadUsers} /> : null}
      {state.status === "empty" ? <StatusPanel text={t("admin.empty")} /> : null}
      {state.data && state.data.items.length ? (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>{t("account.email")}</th>
                <th>{t("account.roles")}</th>
                <th>{t("account.status")}</th>
                <th>{t("admin.users.created")}</th>
                <th>{t("admin.users.lastLogin")}</th>
                <th>{t("admin.table.action")}</th>
              </tr>
            </thead>
            <tbody>
              {state.data.items.map((item) => (
                <tr key={item.userKey}>
                  <td>{item.email}</td>
                  <td>{item.roles.join(", ")}</td>
                  <td><span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span></td>
                  <td>{formatDateTime(item.createdAt)}</td>
                  <td>{item.lastLoginAt ? formatDateTime(item.lastLoginAt) : "-"}</td>
                  <td>
                    <button className="row-action" type="button" onClick={() => void openUser(item.userKey)}>
                      {t("admin.users.manage")}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {selected ? (
        <aside className="admin-drawer" aria-label={t("admin.users.drawer")}>
          <div className="drawer-head">
            <div>
              <strong>{selected.email}</strong>
              <span>{selected.userKey}</span>
            </div>
            <button type="button" onClick={() => setSelected(null)}>{t("coach.back")}</button>
          </div>
          <div className="drawer-grid">
            <InfoLine label={t("account.status")} value={selected.status} />
            <InfoLine label={t("account.locale")} value={selected.locale} />
            <InfoLine label={t("admin.users.timezone")} value={selected.timezone} />
            <InfoLine label={t("admin.users.authVersion")} value={selected.authVersion} />
          </div>
          <div className="drawer-actions">
            <button type="button" onClick={() => void updateStatus(selected.status === "ACTIVE" ? "DISABLED" : "ACTIVE")}>
              {selected.status === "ACTIVE" ? t("admin.users.disable") : t("admin.users.enable")}
            </button>
          </div>
          <label className="field-stack">
            <span>{t("admin.users.roles")}</span>
            <input value={rolesDraft} onChange={(event) => setRolesDraft(event.target.value)} />
          </label>
          <button className="secondary-action compact" type="button" onClick={() => void saveRoles()}>
            {t("admin.users.saveRoles")}
          </button>
          <form className="quota-form" onSubmit={(event) => void saveQuotaPolicy(event)}>
            <label className="field-stack">
              <span>{t("admin.users.dailyOverride")}</span>
              <input type="number" min="0" value={dailyLimitOverride} onChange={(event) => setDailyLimitOverride(event.target.value)} />
            </label>
            <label className="toggle-row">
              <input type="checkbox" checked={unlimited} onChange={(event) => setUnlimited(event.target.checked)} />
              {t("admin.users.unlimited")}
            </label>
            <button className="primary-action compact" type="submit">{t("admin.users.saveQuota")}</button>
          </form>
          <form className="quota-form inline" onSubmit={(event) => void addBonus(event)}>
            <input type="number" min="1" value={bonus} onChange={(event) => setBonus(event.target.value)} aria-label={t("admin.users.bonus")} />
            <button type="submit">{t("admin.users.addBonus")}</button>
          </form>
          <button className="secondary-action compact" type="button" onClick={() => void resetQuota()}>{t("admin.users.resetQuota")}</button>
           {quota ? <QuotaStatePanel quota={quota} /> : <p className="panel-feedback">{t("admin.users.quotaHint")}</p>}
           {feedback ? <p className="form-success">{feedback}</p> : null}
           {operationError ? <p className="form-error">{operationError}</p> : null}
         </aside>
      ) : null}
    </section>
  );
}

function ProvidersPage({ apiClient }: { apiClient: ApiClient }) {
  const { t } = useI18n();
  const [state, setState] = useState<LoadState<AiProvider[]>>({ status: "loading", data: null });

  const load = useCallback(async () => {
    setState((current) => ({ status: "loading", data: current.data }));
    try {
      const providers = await apiClient.listAiProviders();
      setState({ status: providers.length ? "content" : "empty", data: providers });
    } catch {
      setState((current) => ({ status: "error", data: current.data }));
    }
  }, [apiClient]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section className="admin-page">
      <AdminHero eyebrow={t("admin.providers.eyebrow")} title={t("admin.providers.title")} body={t("admin.providers.body")} />
      <NewProviderForm apiClient={apiClient} onSaved={load} />
      {state.status === "loading" ? <StatusPanel text={t("admin.loading")} /> : null}
      {state.status === "error" ? <StatusPanel text={t("admin.error")} actionLabel={t("home.retry")} onAction={load} /> : null}
      {state.status === "empty" ? <StatusPanel text={t("admin.providers.empty")} /> : null}
      {state.data?.length ? (
        <div className="provider-grid">
          {state.data.map((provider) => (
            <ProviderEditor key={provider.providerCode} apiClient={apiClient} provider={provider} onSaved={load} />
          ))}
        </div>
      ) : null}
    </section>
  );
}

function NewProviderForm({ apiClient, onSaved }: { apiClient: ApiClient; onSaved: () => Promise<void> }) {
  const { t } = useI18n();
  const [providerCode, setProviderCode] = useState("deepseek");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      await apiClient.putAiProvider(providerCode.trim(), {
        providerType: "OPENAI_COMPATIBLE",
        displayName: providerCode.trim() || "DeepSeek",
        enabled: true,
        defaultLlm: true,
        defaultAsr: false,
        defaultTts: false,
        baseUrl: "https://api.deepseek.com",
        llmModel: "deepseek-v4-flash",
        asrModel: null,
        ttsModel: null,
        ttsVoice: null,
        timeoutSeconds: 30,
      });
      setFeedback(t("admin.providers.saved"));
      await onSaved();
    } catch {
      setError(t("admin.error"));
    }
  }

  return (
    <form className="admin-inline-create" onSubmit={(event) => void save(event)}>
      <label className="field-stack">
        <span>{t("admin.providers.code")}</span>
        <input value={providerCode} onChange={(event) => setProviderCode(event.target.value)} required />
      </label>
      <button type="submit">{t("admin.providers.upsert")}</button>
      {feedback ? <span>{feedback}</span> : null}
      {error ? <span className="form-error">{error}</span> : null}
    </form>
  );
}

function ProviderEditor({ apiClient, provider, onSaved }: { apiClient: ApiClient; provider: AiProvider; onSaved: () => Promise<void> }) {
  const { t } = useI18n();
  const [draft, setDraft] = useState(() => providerToUpdateRequest(provider));
  const [secret, setSecret] = useState("");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    setDraft(providerToUpdateRequest(provider));
  }, [provider]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      await apiClient.putAiProvider(provider.providerCode, draft);
      setFeedback(t("admin.providers.saved"));
      await onSaved();
    } catch {
      setError(t("admin.error"));
    }
  }

  async function replaceSecret(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      await apiClient.replaceAiProviderSecret(provider.providerCode, { apiKey: secret });
      setSecret("");
      setFeedback(t("admin.providers.secretSaved"));
      await onSaved();
    } catch {
      setError(t("admin.error"));
    }
  }

  return (
    <article className="provider-panel">
      <div className="provider-head">
        <div>
          <strong>{provider.displayName}</strong>
          <span>{provider.providerCode}</span>
        </div>
        <div className="provider-flags">
          {provider.defaultLlm ? <span>{t("admin.providers.defaultLlm")}</span> : null}
          <span className={provider.enabled ? "enabled" : "disabled"}>{provider.enabled ? t("admin.enabled") : t("admin.disabled")}</span>
        </div>
      </div>
      <form className="provider-form" onSubmit={(event) => void save(event)}>
        <label className="field-stack">
          <span>Protocol</span>
          <select
            value={draft.providerType}
            onChange={(event) => setDraft({
              ...draft,
              providerType: event.target.value as AiProviderUpdateRequest["providerType"],
              defaultAsr: event.target.value === "OPENAI" ? draft.defaultAsr : false,
              defaultTts: event.target.value === "OPENAI" ? draft.defaultTts : false,
              asrModel: event.target.value === "OPENAI" ? draft.asrModel : null,
              ttsModel: event.target.value === "OPENAI" ? draft.ttsModel : null,
              ttsVoice: event.target.value === "OPENAI" ? draft.ttsVoice : null,
            })}
          >
            <option value="OPENAI">OpenAI Responses</option>
            <option value="OPENAI_COMPATIBLE">OpenAI Chat Completions compatible</option>
            <option value="GEMINI">Gemini generateContent</option>
          </select>
        </label>
        <label className="field-stack">
          <span>{t("admin.providers.displayName")}</span>
          <input value={draft.displayName} onChange={(event) => setDraft({ ...draft, displayName: event.target.value })} required />
        </label>
        <label className="field-stack">
          <span>{t("admin.providers.baseUrl")}</span>
          <input value={draft.baseUrl} onChange={(event) => setDraft({ ...draft, baseUrl: event.target.value })} required />
        </label>
        <div className="provider-models">
          <label className="field-stack">
            <span>{t("admin.providers.llm")}</span>
            <input value={draft.llmModel} onChange={(event) => setDraft({ ...draft, llmModel: event.target.value })} required />
          </label>
          {draft.providerType === "OPENAI" ? <label className="field-stack">
            <span>{t("admin.providers.asr")}</span>
            <input value={draft.asrModel ?? ""} onChange={(event) => setDraft({ ...draft, asrModel: event.target.value })} required />
          </label> : null}
          {draft.providerType === "OPENAI" ? <label className="field-stack">
            <span>{t("admin.providers.tts")}</span>
            <input value={draft.ttsModel ?? ""} onChange={(event) => setDraft({ ...draft, ttsModel: event.target.value })} required />
          </label> : null}
        </div>
        <div className="admin-checkbox-grid">
          <label><input type="checkbox" checked={draft.enabled} onChange={(event) => setDraft({ ...draft, enabled: event.target.checked })} /> {t("admin.enabled")}</label>
          <label><input type="checkbox" checked={draft.defaultLlm} onChange={(event) => setDraft({ ...draft, defaultLlm: event.target.checked })} /> {t("admin.providers.defaultLlm")}</label>
          {draft.providerType === "OPENAI" ? <label><input type="checkbox" checked={draft.defaultAsr} onChange={(event) => setDraft({ ...draft, defaultAsr: event.target.checked })} /> {t("admin.providers.defaultAsr")}</label> : null}
          {draft.providerType === "OPENAI" ? <label><input type="checkbox" checked={draft.defaultTts} onChange={(event) => setDraft({ ...draft, defaultTts: event.target.checked })} /> {t("admin.providers.defaultTts")}</label> : null}
        </div>
        <button className="primary-action compact" type="submit">{t("admin.save")}</button>
      </form>
      <form className="secret-form" onSubmit={(event) => void replaceSecret(event)}>
        <label className="field-stack">
          <span>{t("admin.providers.apiKey")}</span>
          <input value={secret} type="password" onChange={(event) => setSecret(event.target.value)} placeholder={provider.apiKeyMaskedHint ?? t("admin.providers.notConfigured")} required />
        </label>
        <button type="submit">{t("admin.providers.replaceSecret")}</button>
      </form>
      <p className="panel-feedback">{provider.apiKeyConfigured ? t("admin.providers.secretConfigured") : t("admin.providers.secretMissing")}</p>
      {feedback ? <p className="form-success">{feedback}</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
    </article>
  );
}

function SettingsPage({ apiClient }: { apiClient: ApiClient }) {
  const { t } = useI18n();
  const [state, setState] = useState<LoadState<AdminSystemSetting[]>>({ status: "loading", data: null });

  const load = useCallback(async () => {
    setState((current) => ({ status: "loading", data: current.data }));
    try {
      const settings = await apiClient.listAdminSystemSettings();
      setState({ status: settings.length ? "content" : "empty", data: settings });
    } catch {
      setState((current) => ({ status: "error", data: current.data }));
    }
  }, [apiClient]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section className="admin-page">
      <AdminHero eyebrow={t("admin.settings.eyebrow")} title={t("admin.settings.title")} body={t("admin.settings.body")} />
      {state.status === "loading" ? <StatusPanel text={t("admin.loading")} /> : null}
      {state.status === "error" ? <StatusPanel text={t("admin.error")} actionLabel={t("home.retry")} onAction={load} /> : null}
      {state.status === "empty" ? <StatusPanel text={t("admin.empty")} /> : null}
      <div className="settings-admin-list">
        {state.data?.map((setting) => (
          <SettingEditor key={setting.key} apiClient={apiClient} setting={setting} onSaved={load} />
        ))}
      </div>
    </section>
  );
}

function SettingEditor({ apiClient, setting, onSaved }: { apiClient: ApiClient; setting: AdminSystemSetting; onSaved: () => Promise<void> }) {
  const { t } = useI18n();
  const [value, setValue] = useState(setting.value);
  const [valueType, setValueType] = useState(setting.valueType);
  const [description, setDescription] = useState(setting.description);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      await apiClient.updateAdminSystemSetting(setting.key, { value, valueType, description });
      setFeedback(t("admin.settings.saved"));
      await onSaved();
    } catch {
      setError(t("admin.error"));
    }
  }

  return (
    <form className="setting-row-editor" onSubmit={(event) => void save(event)}>
      <div>
        <strong>{setting.key}</strong>
        <span>{description}</span>
      </div>
      <input value={value} onChange={(event) => setValue(event.target.value)} />
      <select value={valueType} onChange={(event) => setValueType(event.target.value as AdminSystemSetting["valueType"])}>
        <option value="STRING">STRING</option>
        <option value="INTEGER">INTEGER</option>
        <option value="BOOLEAN">BOOLEAN</option>
        <option value="JSON">JSON</option>
      </select>
      <input value={description} onChange={(event) => setDescription(event.target.value)} />
      <button type="submit">{t("admin.save")}</button>
      {feedback ? <span>{feedback}</span> : null}
      {error ? <span className="form-error">{error}</span> : null}
    </form>
  );
}

function AuditPage({ apiClient }: { apiClient: ApiClient }) {
  const { t } = useI18n();
  const [page, setPage] = useState(0);
  const [state, setState] = useState<LoadState<AdminAuditPage>>({ status: "loading", data: null });

  const load = useCallback(async () => {
    setState((current) => ({ status: "loading", data: current.data }));
    try {
      const audit = await apiClient.listAdminAudit({ page, size: 20 });
      setState({ status: audit.items.length ? "content" : "empty", data: audit });
    } catch {
      setState((current) => ({ status: "error", data: current.data }));
    }
  }, [apiClient, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const totalPages = state.data ? Math.max(1, Math.ceil(state.data.total / state.data.size)) : 1;

  return (
    <section className="admin-page">
      <AdminHero eyebrow={t("admin.audit.eyebrow")} title={t("admin.audit.title")} body={t("admin.audit.body")} />
      {state.status === "loading" ? <StatusPanel text={t("admin.loading")} /> : null}
      {state.status === "error" ? <StatusPanel text={t("admin.error")} actionLabel={t("home.retry")} onAction={load} /> : null}
      {state.status === "empty" ? <StatusPanel text={t("admin.empty")} /> : null}
      {state.data?.items.length ? (
        <div className="audit-list">
          {state.data.items.map((item) => (
            <article className="audit-item" key={item.id}>
              <div>
                <strong>{item.actionCode}</strong>
                <p>{item.actorEmail ?? "-"} {"->"} {item.targetType}:{item.targetKey}</p>
              </div>
              <time>{formatDateTime(item.createdAt)}</time>
            </article>
          ))}
        </div>
      ) : null}
      <div className="pagination-row">
        <button type="button" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>{t("admin.prev")}</button>
        <span>{page + 1} / {totalPages}</span>
        <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)}>{t("admin.next")}</button>
      </div>
    </section>
  );
}

function AdminHero({ eyebrow, title, body }: { eyebrow: string; title: string; body: string }) {
  return (
    <div className="admin-hero">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="summary">{body}</p>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <article className="metric-tile">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function QuickAction({ title, body, onClick }: { title: string; body: string; onClick: () => void }) {
  return (
    <button className="quick-action" type="button" onClick={onClick}>
      <strong>{title}</strong>
      <span>{body}</span>
    </button>
  );
}

function StatusPanel({ text, actionLabel, onAction }: { text: string; actionLabel?: string; onAction?: () => void | Promise<void> }) {
  return (
    <article className="admin-status-panel">
      <p>{text}</p>
      {actionLabel && onAction ? <button type="button" onClick={() => void onAction()}>{actionLabel}</button> : null}
    </article>
  );
}

function InfoLine({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="info-line">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function QuotaStatePanel({ quota }: { quota: AdminQuotaState }) {
  return (
    <article className="quota-state-panel">
      <div className="quota-meter" aria-label="Admin quota meter">
        <div style={{ width: `${quotaUsagePercent(quota)}%` }} />
      </div>
      <div className="drawer-grid">
        <InfoLine label="Date" value={quota.quotaDate} />
        <InfoLine label="Limit" value={quota.unlimited ? "Unlimited" : quota.dailyLimit} />
        <InfoLine label="Used" value={quota.used} />
        <InfoLine label="Reserved" value={quota.reserved} />
        <InfoLine label="Bonus" value={quota.bonus} />
        <InfoLine label="Remaining" value={quota.unlimited ? "Unlimited" : quota.remaining} />
      </div>
    </article>
  );
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString();
}
