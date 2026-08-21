import { useCallback, useEffect, useState, type ReactNode } from "react";
import type { ApiClient, LessonAttemptReceipt, LessonSession, LessonStep } from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import {
  buildScenarioLesson,
  isSpeakingStep,
  lessonLoaded,
  taskHeroStyle,
  withMediaFailure,
  type ScenarioLessonLoadedState,
} from "./scenarioLessonModel";

type PageState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "content"; value: ScenarioLessonLoadedState };

export function ScenarioLessonPage({
  apiClient,
  sessionId,
  onBack,
}: {
  apiClient: ApiClient;
  sessionId: string;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const [state, setState] = useState<PageState>({ status: "loading" });

  const load = useCallback(async (signal?: AbortSignal) => {
    setState({ status: "loading" });
    try {
      const session = await apiClient.getLessonSession(sessionId, { signal });
      const resource = await apiClient.getLearningResourceVersion(
        session.resource.resourceId,
        session.resource.resourceVersion,
        { signal },
      );
      const lesson = buildScenarioLesson(resource);
      const [imageResult, audioResult] = await Promise.allSettled([
        apiClient.createLearningResourceMediaAccess(resource.resourceId, {
          assetId: lesson.taskHero.assetId,
          purpose: "DISPLAY",
        }, { signal }),
        lesson.audioAssetId
          ? apiClient.createLearningResourceMediaAccess(resource.resourceId, {
              assetId: lesson.audioAssetId,
              purpose: "PLAYBACK",
            }, { signal })
          : Promise.reject(new Error("No scene dialogue audio")),
      ]);
      if (signal?.aborted) return;
      setState({
        status: "content",
        value: lessonLoaded(session, resource, {
          imageUrl: imageResult.status === "fulfilled" ? imageResult.value.url : undefined,
          audioUrl: audioResult.status === "fulfilled" ? audioResult.value.url : undefined,
        }),
      });
    } catch (error) {
      if (signal?.aborted) return;
      setState({ status: "error", message: error instanceof Error ? error.message : t("lesson.error.desc") });
    }
  }, [apiClient, sessionId, t]);

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  if (state.status === "loading") {
    return <LessonStatus title={t("lesson.loading.title")} description={t("lesson.loading.desc")} busy onBack={onBack} />;
  }
  if (state.status === "error") {
    return (
      <LessonStatus title={t("lesson.error.title")} description={state.message} onBack={onBack}>
        <button className="primary-action" type="button" onClick={() => void load()}>{t("lesson.retry")}</button>
      </LessonStatus>
    );
  }

  async function updateSession(action: (session: LessonSession) => Promise<LessonSession>) {
    if (state.status !== "content") return;
    const current = state.value;
    try {
      const session = await action(current.session);
      setState({ status: "content", value: { ...current, session } });
    } catch (error) {
      setState({ status: "error", message: error instanceof Error ? error.message : t("lesson.error.desc") });
    }
  }

  async function submitAttempt(taskId: string, text: string): Promise<LessonAttemptReceipt> {
    if (state.status !== "content") throw new Error(t("lesson.error.desc"));
    const current = state.value;
    const receipt = await apiClient.submitLessonAttempt(current.session.sessionId, {
      taskId,
      inputType: "TEXT",
      text,
      clientStartedAt: new Date().toISOString(),
    });
    const session = await apiClient.getLessonSession(current.session.sessionId);
    setState({ status: "content", value: { ...current, session } });
    return receipt;
  }

  return (
    <ScenarioLessonContent
      value={state.value}
      onBack={onBack}
      onImageError={() => setState((current) => current.status === "content"
        ? { status: "content", value: withMediaFailure(current.value, "image") }
        : current)}
      onAudioError={() => setState((current) => current.status === "content"
        ? { status: "content", value: withMediaFailure(current.value, "audio") }
        : current)}
      onPause={() => updateSession((session) => apiClient.pauseLessonSession(session.sessionId))}
      onResume={() => updateSession((session) => apiClient.resumeLessonSession(session.sessionId))}
      onCompleteStep={(step) => updateSession((session) => apiClient.completeLessonStep(session.sessionId, step))}
      onSubmitAttempt={submitAttempt}
    />
  );
}

export function ScenarioLessonContent({
  value,
  onBack,
  onImageError,
  onAudioError,
  onPause,
  onResume,
  onCompleteStep,
  onSubmitAttempt,
}: {
  value: ScenarioLessonLoadedState;
  onBack: () => void;
  onImageError: () => void;
  onAudioError: () => void;
  onPause: () => void;
  onResume: () => void;
  onCompleteStep: (step: LessonStep) => void;
  onSubmitAttempt: (taskId: string, text: string) => Promise<LessonAttemptReceipt>;
}) {
  const { t } = useI18n();
  const [transcriptVisible, setTranscriptVisible] = useState(false);
  const [advancing, setAdvancing] = useState(false);
  const [attemptText, setAttemptText] = useState("");
  const [attemptSubmitting, setAttemptSubmitting] = useState(false);
  const [attemptError, setAttemptError] = useState<string>();
  const [objectiveResult, setObjectiveResult] = useState<LessonAttemptReceipt["objectiveResult"]>();
  const { session, lesson } = value;
  const heroStyle = taskHeroStyle(lesson.taskHero);
  const isPaused = session.status === "PAUSED";

  useEffect(() => {
    setAdvancing(false);
    setAttemptError(undefined);
    setObjectiveResult(undefined);
    setAttemptText("");
  }, [session.currentStep]);

  const remainingQuestionId = session.attemptProgress?.remainingTaskIds[0];
  const activeQuestion = lesson.questions.find((question) => question.questionId === remainingQuestionId)
    ?? lesson.questions[0];

  async function submitCurrentAttempt() {
    const taskId = session.currentStep === "COMPREHENSION" ? activeQuestion?.questionId : lesson.guidedSpeaking?.taskId;
    if (!taskId || !attemptText.trim() || attemptSubmitting || isPaused) return;
    setAttemptSubmitting(true);
    setAttemptError(undefined);
    try {
      const receipt = await onSubmitAttempt(taskId, attemptText.trim());
      setObjectiveResult(receipt.objectiveResult);
      setAttemptText("");
    } catch (error) {
      setAttemptError(error instanceof Error ? error.message : t("lesson.attempt.error"));
    } finally {
      setAttemptSubmitting(false);
    }
  }

  function completeStep() {
    if (!session.step.clientCompletable || advancing || isPaused) return;
    setAdvancing(true);
    onCompleteStep(session.currentStep);
  }

  return (
    <main className="scenario-lesson-shell">
      <header className="scenario-lesson-topbar">
        <button type="button" onClick={onBack} aria-label={t("lesson.back")}>← {t("lesson.back")}</button>
        <div>
          <strong>{lesson.seasonId} · {lesson.episodeId}</strong>
          <span>{t(`lesson.step.${session.currentStep}`)}</span>
        </div>
        {isPaused ? (
          <button type="button" onClick={onResume}>{t("lesson.resume")}</button>
        ) : (
          <button type="button" onClick={onPause}>{t("lesson.pause")}</button>
        )}
      </header>

      <div className="scenario-progress" role="progressbar" aria-label={t("lesson.progress")} aria-valuemin={0}
        aria-valuemax={session.progress.totalRequiredSteps} aria-valuenow={session.progress.completedSteps}>
        <span style={{ width: `${Math.round((session.progress.completedSteps / session.progress.totalRequiredSteps) * 100)}%` }} />
      </div>

      <section className="scenario-lesson-grid" aria-labelledby="scenario-lesson-title">
        <figure className="scenario-stage">
          {value.imageUrl && !value.imageUnavailable ? (
            <img src={value.imageUrl} alt={lesson.taskHero.altText} style={heroStyle} onError={onImageError} />
          ) : (
            <div className="scenario-image-fallback" role="img" aria-label={lesson.taskHero.altText} style={{ aspectRatio: heroStyle.aspectRatio }}>
              <span aria-hidden="true">LM</span>
              <div><strong>Lin Muen</strong><small>{t("lesson.imageFallback")}</small></div>
            </div>
          )}
          <figcaption>
            <span>{humanizeCode(lesson.scene)}</span>
            <strong>Lin Muen · {lesson.story.title}</strong>
          </figcaption>
          <div className="scenario-overlay" aria-label={t("lesson.sceneFacts")}>
            <span>{lesson.level}</span><span>{lesson.topic}</span><span>{lesson.communicationGoal}</span>
          </div>
        </figure>

        <article className="scenario-panel">
          {isPaused ? <div className="lesson-paused" role="status">{t("lesson.paused")}</div> : null}
          <StepContent value={value} transcriptVisible={transcriptVisible} setTranscriptVisible={setTranscriptVisible}
            onAudioError={onAudioError} attemptText={attemptText} setAttemptText={setAttemptText}
            attemptSubmitting={attemptSubmitting} attemptError={attemptError} objectiveResult={objectiveResult}
            activeQuestion={activeQuestion} onSubmitAttempt={submitCurrentAttempt} />
          {session.step.clientCompletable ? (
            <button className="scenario-primary-action" type="button" disabled={isPaused || advancing} onClick={completeStep}>
              {advancing ? t("lesson.advancing") : t(`lesson.continue.${session.currentStep}`)}
            </button>
          ) : session.currentStep !== "COMPREHENSION" && session.currentStep !== "GUIDED_SPEAKING" ? (
            <p className="scenario-step-gate">{isSpeakingStep(session.currentStep) ? t("lesson.speakingNext") : t("lesson.nextTask")}</p>
          ) : null}
        </article>
      </section>
    </main>
  );
}

function StepContent({
  value,
  transcriptVisible,
  setTranscriptVisible,
  onAudioError,
  attemptText,
  setAttemptText,
  attemptSubmitting,
  attemptError,
  objectiveResult,
  activeQuestion,
  onSubmitAttempt,
}: {
  value: ScenarioLessonLoadedState;
  transcriptVisible: boolean;
  setTranscriptVisible: (visible: boolean) => void;
  onAudioError: () => void;
  attemptText: string;
  setAttemptText: (value: string) => void;
  attemptSubmitting: boolean;
  attemptError?: string;
  objectiveResult?: LessonAttemptReceipt["objectiveResult"];
  activeQuestion?: ScenarioLessonLoadedState["lesson"]["questions"][number];
  onSubmitAttempt: () => void;
}) {
  const { t } = useI18n();
  const { lesson, session } = value;
  const showListening = session.currentStep === "FIRST_LISTEN";
  const showLanguage = session.currentStep === "TRANSCRIPT_EXPRESSIONS" || showListening || value.audioUnavailable;

  if (session.currentStep === "SCENE_CONTEXT") {
    return (
      <>
        <p className="scene-kicker">{t("lesson.sceneContext")}</p>
        <h1 id="scenario-lesson-title">{lesson.story.title}</h1>
        <p className="scenario-context">{lesson.story.context}</p>
        <section className="scenario-mission" aria-labelledby="scenario-mission-title">
          <span aria-hidden="true">→</span>
          <div><h2 id="scenario-mission-title">{t("lesson.mission")}</h2><p>{lesson.story.mission}</p></div>
        </section>
      </>
    );
  }

  if (session.currentStep === "COMPREHENSION" && activeQuestion) {
    return (
      <>
        <p className="scene-kicker">{t("lesson.step.COMPREHENSION")}</p>
        <h1 id="scenario-lesson-title">{t("lesson.comprehension.title")}</h1>
        <p className="scenario-context">{activeQuestion.prompt}</p>
        <label className="lesson-attempt-field">
          <span>{t("lesson.comprehension.answer")}</span>
          <input value={attemptText} onChange={(event) => setAttemptText(event.target.value)}
            disabled={attemptSubmitting || session.status === "PAUSED"} />
        </label>
        {objectiveResult ? (
          <div className="lesson-attempt-result" role="status">
            <strong>{objectiveResult.correct ? t("lesson.comprehension.correct") : t("lesson.comprehension.review")}</strong>
            <span>{objectiveResult.correct ? objectiveResult.explanation : `${objectiveResult.explanation} ${objectiveResult.expectedAnswer}`}</span>
          </div>
        ) : null}
        {attemptError ? <p className="lesson-attempt-error" role="alert">{attemptError}</p> : null}
        <button className="scenario-primary-action" type="button" disabled={!attemptText.trim() || attemptSubmitting || session.status === "PAUSED"}
          onClick={onSubmitAttempt}>{attemptSubmitting ? t("lesson.attempt.submitting") : t("lesson.comprehension.submit")}</button>
      </>
    );
  }

  if (session.currentStep === "GUIDED_SPEAKING" && lesson.guidedSpeaking) {
    return (
      <>
        <p className="scene-kicker">{t("lesson.step.GUIDED_SPEAKING")}</p>
        <h1 id="scenario-lesson-title">{t("lesson.guided.title")}</h1>
        <p className="scenario-context">{lesson.guidedSpeaking.prompt}</p>
        <ul className="scenario-expressions">
          {lesson.guidedSpeaking.scaffolding.map((hint) => <li key={hint}><span>{hint}</span></li>)}
        </ul>
        <label className="lesson-attempt-field">
          <span>{t("lesson.guided.response")}</span>
          <textarea rows={5} value={attemptText} onChange={(event) => setAttemptText(event.target.value)}
            disabled={attemptSubmitting || session.status === "PAUSED"} />
        </label>
        {attemptError ? <p className="lesson-attempt-error" role="alert">{attemptError}</p> : null}
        <button className="scenario-primary-action" type="button" disabled={!attemptText.trim() || attemptSubmitting || session.status === "PAUSED"}
          onClick={onSubmitAttempt}>{attemptSubmitting ? t("lesson.attempt.submitting") : t("lesson.guided.submit")}</button>
      </>
    );
  }

  return (
    <>
      <p className="scene-kicker">{t(`lesson.step.${session.currentStep}`)}</p>
      <h1 id="scenario-lesson-title">
        {showListening ? t("lesson.firstListen.title") : t(`lesson.stageTitle.${stageTitleKey(session.currentStep)}`)}
      </h1>
      {showListening ? <p className="scenario-context">{t("lesson.firstListen.desc")}</p> : null}
      {showListening ? (
        value.audioUrl && !value.audioUnavailable ? (
          <audio className="scenario-audio" controls preload="metadata" src={value.audioUrl} onError={onAudioError}
            aria-label={t("lesson.audioLabel")} />
        ) : (
          <div className="media-fallback-notice" role="alert"><strong>{t("lesson.audioFallback.title")}</strong><span>{t("lesson.audioFallback.desc")}</span></div>
        )
      ) : null}

      {showLanguage ? (
        <section className="language-support">
          <button type="button" aria-expanded={transcriptVisible} aria-controls="scenario-transcript"
            onClick={() => setTranscriptVisible(!transcriptVisible)}>
            {transcriptVisible ? t("lesson.transcript.hide") : t("lesson.transcript.show")}
          </button>
          {transcriptVisible ? (
            <div id="scenario-transcript">
              <h2>{t("lesson.transcript.title")}</h2>
              <ol className="scenario-transcript">
                {lesson.transcript.map((sentence) => (
                  <li key={sentence.sentenceId}><strong>{sentence.speaker}</strong><p>{sentence.text}</p></li>
                ))}
              </ol>
              <h2>{t("lesson.expressions.title")}</h2>
              <ul className="scenario-expressions">
                {lesson.expressions.map((expression) => (
                  <li key={expression.expression}><strong>{expression.expression}</strong><span>{expression.meaningZh}</span><small>{expression.usage}</small></li>
                ))}
              </ul>
            </div>
          ) : <p className="transcript-hidden-note">{t("lesson.transcript.hidden")}</p>}
        </section>
      ) : null}

      {!showListening && !showLanguage ? (
        <section className="upcoming-lesson-step">
          <h2>{isSpeakingStep(session.currentStep) ? t("lesson.speakingNext") : t("lesson.nextTask")}</h2>
          <p>{lesson.story.mission}</p>
          {session.attemptProgress?.pendingAttemptId ? <p role="status">{t("lesson.attempt.pending")}</p> : null}
        </section>
      ) : null}
    </>
  );
}

function LessonStatus({
  title,
  description,
  busy = false,
  onBack,
  children,
}: {
  title: string;
  description: string;
  busy?: boolean;
  onBack: () => void;
  children?: ReactNode;
}) {
  const { t } = useI18n();
  return (
    <main className="app-shell" aria-busy={busy}>
      <section className="hero"><p className="eyebrow">Scenario Lesson</p><h1>{title}</h1><p className="summary">{description}</p>{children}
        <button className="text-button" type="button" onClick={onBack}>← {t("lesson.back")}</button>
      </section>
    </main>
  );
}

function stageTitleKey(step: LessonStep): string {
  if (step === "TRANSCRIPT_EXPRESSIONS") return "language";
  if (isSpeakingStep(step)) return "speaking";
  return "upcoming";
}

function humanizeCode(value: string): string {
  return value.replace(/[._-]+/g, " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
