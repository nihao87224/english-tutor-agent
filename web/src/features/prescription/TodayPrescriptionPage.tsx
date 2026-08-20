import { useCallback, useEffect, useState, type ReactNode } from "react";
import type {
  ApiClient,
  DailyLearningPrescription,
  PrescriptionBlock,
  PrescriptionRegenerationReason,
  PrescriptionTaskHero,
  QuotaStatus,
} from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import {
  buildRegenerationRequest,
  createPrescriptionIdempotencyKey,
  prescriptionFailed,
  prescriptionLoaded,
  primaryPrescriptionBlock,
  taskHeroStyle,
  type PrescriptionViewState,
} from "./prescriptionModel";

interface TodayPrescriptionPageProps {
  apiClient: ApiClient;
  quota: QuotaStatus | null;
  quotaLoading: boolean;
  timezone: string;
  onRefreshQuota: () => Promise<void>;
  onOpenAccount: () => void;
}

const AVAILABLE_MINUTES = [5, 10, 15, 20] as const;

export function TodayPrescriptionPage({
  apiClient,
  quota,
  quotaLoading,
  timezone,
  onRefreshQuota,
  onOpenAccount,
}: TodayPrescriptionPageProps) {
  const { t } = useI18n();
  const [viewState, setViewState] = useState<PrescriptionViewState>({ status: "loading" });
  const [availableMinutes, setAvailableMinutes] = useState<number>(10);

  const loadPrescription = useCallback(
    async (signal?: AbortSignal) => {
      setViewState({ status: "loading" });
      try {
        const prescription = await apiClient.getTodayPrescription({ timezone }, { signal });
        setViewState(prescriptionLoaded(prescription));
      } catch (error) {
        if (signal?.aborted) return;
        setViewState(prescriptionFailed(error));
      }
    },
    [apiClient, timezone],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadPrescription(controller.signal);
    return () => controller.abort();
  }, [loadPrescription]);

  async function regenerate(reason: PrescriptionRegenerationReason) {
    if (viewState.status !== "content" || viewState.regenerating) return;
    const current = viewState.prescription;
    setViewState({ status: "content", prescription: current, regenerating: true });
    try {
      const next = await apiClient.regenerateTodayPrescription(
        buildRegenerationRequest(current, reason, availableMinutes),
        { idempotencyKey: createPrescriptionIdempotencyKey() },
      );
      const loaded = prescriptionLoaded(next);
      setViewState(
        loaded.status === "content"
          ? { ...loaded, notice: t("prescription.feedback.updated") }
          : loaded,
      );
    } catch (error) {
      setViewState(prescriptionFailed(error, current));
    }
  }

  if (viewState.status === "loading") {
    return <PrescriptionStatus title={t("prescription.loading.title")} description={t("prescription.loading.desc")} busy />;
  }
  if (viewState.status === "empty") {
    return (
      <PrescriptionStatus title={t("prescription.empty.title")} description={viewState.message ?? t("prescription.empty.desc")}>
        <button className="secondary-action" type="button" onClick={() => void loadPrescription()}>{t("prescription.refresh")}</button>
      </PrescriptionStatus>
    );
  }
  if (viewState.status === "fallback") {
    return (
      <PrescriptionStatus title={t("prescription.fallback.title")} description={t("prescription.fallback.desc")}>
        <p className="prescription-status-detail">{viewState.message}</p>
        <button className="secondary-action" type="button" onClick={() => void loadPrescription()}>{t("prescription.refresh")}</button>
      </PrescriptionStatus>
    );
  }
  if (viewState.status === "error") {
    return (
      <PrescriptionStatus title={t("prescription.error.title")} description={viewState.message}>
        <button className="secondary-action" type="button" onClick={() => void loadPrescription()}>{t("prescription.retry")}</button>
      </PrescriptionStatus>
    );
  }
  if (viewState.status === "stale") {
    return (
      <PrescriptionStatus title={t("prescription.stale.title")} description={t("prescription.stale.desc")}>
        <button className="primary-action" type="button" onClick={() => void loadPrescription()}>{t("prescription.stale.action")}</button>
      </PrescriptionStatus>
    );
  }

  return (
    <PrescriptionContent
      prescription={viewState.prescription}
      regenerating={viewState.regenerating}
      notice={viewState.notice}
      quota={quota}
      quotaLoading={quotaLoading}
      availableMinutes={availableMinutes}
      onAvailableMinutesChange={setAvailableMinutes}
      onRegenerate={regenerate}
      onRefreshQuota={onRefreshQuota}
      onOpenAccount={onOpenAccount}
    />
  );
}

export function PrescriptionContent({
  prescription,
  regenerating,
  notice,
  quota,
  quotaLoading,
  availableMinutes,
  onAvailableMinutesChange,
  onRegenerate,
  onRefreshQuota,
  onOpenAccount,
}: {
  prescription: DailyLearningPrescription;
  regenerating: boolean;
  notice?: string;
  quota: QuotaStatus | null;
  quotaLoading: boolean;
  availableMinutes: number;
  onAvailableMinutesChange: (minutes: number) => void;
  onRegenerate: (reason: PrescriptionRegenerationReason) => void;
  onRefreshQuota: () => Promise<void>;
  onOpenAccount: () => void;
}) {
  const { t, locale } = useI18n();
  const primaryBlock = primaryPrescriptionBlock(prescription);
  const blocks = [...prescription.blocks].sort((left, right) => left.sequence - right.sequence);

  return (
    <section className="prescription-page" aria-labelledby="prescription-title">
      <header className="prescription-heading">
        <div>
          <p className="eyebrow">{t("prescription.eyebrow")}</p>
          <h1 id="prescription-title">{prescription.priorityGoal.label}</h1>
          <p className="summary">{prescription.rationale}</p>
        </div>
        <div className="prescription-time" aria-label={t("prescription.totalTime", { minutes: prescription.estimatedMinutes })}>
          <strong>{prescription.estimatedMinutes}</strong>
          <span>{t("prescription.minutes")}</span>
        </div>
      </header>

      {primaryBlock ? (
        <div className="prescription-main-grid">
          <TaskHero key={primaryBlock.taskHero.assetId} hero={primaryBlock.taskHero} experienceTitle={prescription.experience.title} />
          <article className="prescription-mission">
            <p className="scene-kicker">{prescription.experience.seasonId} · {prescription.experience.episodeId}</p>
            <h2>{primaryBlock.title}</h2>
            <p>{prescription.experience.title} · {humanizeCode(prescription.experience.sceneId)}</p>
            <dl className="mission-facts">
              <div><dt>{t("prescription.skill")}</dt><dd>{primaryBlock.skillUnitVariantId}</dd></div>
              <div><dt>{t("prescription.level")}</dt><dd>{primaryBlock.difficulty} · {t(`prescription.scaffolding.${primaryBlock.scaffolding}`)}</dd></div>
              <div><dt>{t("prescription.evidence")}</dt><dd>{primaryBlock.expectedEvidence.map(humanizeCode).join(" · ")}</dd></div>
            </dl>
          </article>
        </div>
      ) : null}

      <section className="prescription-reason" aria-labelledby="prescription-reason-title">
        <span aria-hidden="true">i</span>
        <div>
          <h2 id="prescription-reason-title">{t("prescription.reason.title")}</h2>
          <p>{prescription.rationale}</p>
          <div className="reason-codes">{prescription.reasonCodes.map((code) => <span key={code}>{humanizeCode(code)}</span>)}</div>
        </div>
      </section>

      <div className="prescription-detail-grid">
        <section className="prescription-blocks" aria-labelledby="prescription-blocks-title">
          <div className="prescription-section-heading">
            <h2 id="prescription-blocks-title">{t("prescription.blocks.title")}</h2>
            <span>{t("prescription.version", { version: prescription.version })}</span>
          </div>
          <ol>
            {blocks.map((block) => <PrescriptionBlockCard block={block} key={block.blockId} />)}
          </ol>
        </section>

        <aside className="prescription-controls">
          <fieldset disabled={regenerating}>
            <legend>{t("prescription.feedback.title")}</legend>
            <p>{t("prescription.feedback.desc")}</p>
            <div className="feedback-group">
              <span>{t("prescription.feedback.difficulty")}</span>
              <div>
                <button type="button" onClick={() => onRegenerate("TOO_HARD")}>{t("prescription.feedback.hard")}</button>
                <button type="button" onClick={() => onRegenerate("TOO_EASY")}>{t("prescription.feedback.easy")}</button>
              </div>
            </div>
            <div className="feedback-group feedback-time-group">
              <label htmlFor="available-minutes">{t("prescription.feedback.time")}</label>
              <div>
                <select id="available-minutes" value={availableMinutes} onChange={(event) => onAvailableMinutesChange(Number(event.target.value))}>
                  {AVAILABLE_MINUTES.map((minutes) => <option value={minutes} key={minutes}>{t("prescription.feedback.minutes", { minutes })}</option>)}
                </select>
                <button type="button" onClick={() => onRegenerate("TIME_INSUFFICIENT")}>{t("prescription.feedback.recompose")}</button>
              </div>
            </div>
            <button className="feedback-topic" type="button" onClick={() => onRegenerate("TOPIC_REJECTED")}>{t("prescription.feedback.topic")}</button>
          </fieldset>
          <p className="prescription-live-status" aria-live="polite">
            {regenerating ? t("prescription.feedback.updating") : notice ?? ""}
          </p>
          <QuotaSummary
            quota={quota}
            loading={quotaLoading}
            locale={locale}
            onRefresh={onRefreshQuota}
            onOpenAccount={onOpenAccount}
          />
        </aside>
      </div>
    </section>
  );
}

function TaskHero({ hero, experienceTitle }: { hero: PrescriptionTaskHero; experienceTitle: string }) {
  const { t } = useI18n();
  const [failed, setFailed] = useState(false);
  const style = taskHeroStyle(hero);
  return (
    <figure className="prescription-hero">
      {hero.url && !failed ? (
        <img src={hero.url} alt={hero.altText} style={style} onError={() => setFailed(true)} />
      ) : (
        <div className="prescription-hero-fallback" role="img" aria-label={hero.altText} style={{ aspectRatio: style.aspectRatio }}>
          <span>LM</span>
          <strong>{t("prescription.heroFallback")}</strong>
        </div>
      )}
      <figcaption><strong>Lin Muen</strong><span>{experienceTitle}</span></figcaption>
    </figure>
  );
}

function PrescriptionBlockCard({ block }: { block: PrescriptionBlock }) {
  const { t } = useI18n();
  return (
    <li className={block.status === "SKIPPED" ? "is-skipped" : ""}>
      <span className="block-sequence">{block.sequence}</span>
      <div>
        <span className="block-type">{t(`prescription.blockType.${block.type}`)}</span>
        <h3>{block.title}</h3>
        <p>{humanizeCode(block.trainingType)} · {block.resource.resourceVersion}</p>
        {block.fallbackResource ? <small>{t("prescription.blockFallback")}</small> : null}
      </div>
      <strong className="block-time">{block.estimatedMinutes} {t("prescription.minShort")}</strong>
    </li>
  );
}

function QuotaSummary({
  quota,
  loading,
  locale,
  onRefresh,
  onOpenAccount,
}: {
  quota: QuotaStatus | null;
  loading: boolean;
  locale: string;
  onRefresh: () => Promise<void>;
  onOpenAccount: () => void;
}) {
  const { t } = useI18n();
  return (
    <section className="prescription-quota" aria-labelledby="prescription-quota-title">
      <div><h2 id="prescription-quota-title">{t("home.quota")}</h2><strong>{quota ? (quota.unlimited ? t("home.quotaUnlimited") : t("home.quotaRemaining", { remaining: quota.remaining })) : loading ? "…" : "–"}</strong></div>
      {quota ? <p>{t("home.quotaUsed", { used: quota.used, limit: quota.dailyLimit + quota.bonus })} · {new Date(quota.resetAt).toLocaleString(locale)}</p> : null}
      <div>
        <button type="button" onClick={() => void onRefresh()}>{t("home.refresh")}</button>
        <button type="button" onClick={onOpenAccount}>{t("app.nav.account")}</button>
      </div>
    </section>
  );
}

function PrescriptionStatus({ title, description, busy = false, children }: { title: string; description: string; busy?: boolean; children?: ReactNode }) {
  const { t } = useI18n();
  return (
    <section className="app-shell prescription-status" aria-busy={busy}>
      <section className="hero">
        <p className="eyebrow">{t("prescription.eyebrow")}</p>
        <h1>{title}</h1>
        <p className="summary">{description}</p>
        {children}
      </section>
    </section>
  );
}

function humanizeCode(value: string): string {
  return value.replace(/[._-]+/g, " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
