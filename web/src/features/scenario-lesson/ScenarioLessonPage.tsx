import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import type {
  ApiClient, LessonAttemptReceipt, LessonSession, LessonStep, RolePlayMessageRequest,
  RolePlayTurn, SseEvent, SseEventHandler, LessonFeedbackCompletion,
} from "../../shared/api";
import { useI18n } from "../../shared/i18n";
import { FeedbackPanel } from "./FeedbackPanel";
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

  async function submitAudioAttempt(taskId: string, file: Blob, durationMs: number): Promise<LessonAttemptReceipt> {
    if (state.status !== "content") throw new Error(t("lesson.error.desc"));
    const current = state.value;
    const asset = await apiClient.uploadAudio({ file, durationMs, purpose: "LESSON_ATTEMPT" });
    const receipt = await apiClient.submitLessonAttempt(current.session.sessionId, {
      taskId,
      inputType: "AUDIO",
      audioAssetId: asset.audioAssetId,
      clientStartedAt: new Date(Date.now() - durationMs).toISOString(),
      clientDurationMs: durationMs,
    });
    const session = await apiClient.getLessonSession(current.session.sessionId);
    setState({ status: "content", value: { ...current, session } });
    return receipt;
  }

  async function confirmTranscript(
    attemptId: string,
    decision: "CONFIRM" | "CORRECT" | "RE_RECORD",
    correctedText?: string,
  ): Promise<LessonAttemptReceipt> {
    if (state.status !== "content") throw new Error(t("lesson.error.desc"));
    const current = state.value;
    const receipt = await apiClient.confirmLessonAttemptTranscript(current.session.sessionId, attemptId, {
      decision,
      correctedText,
    });
    const session = await apiClient.getLessonSession(current.session.sessionId);
    setState({ status: "content", value: { ...current, session } });
    return receipt;
  }

  async function completeFeedback(attemptId: string): Promise<LessonFeedbackCompletion> {
    if (state.status !== "content") throw new Error(t("lesson.error.desc"));
    const current = state.value;
    const completion = await apiClient.completeLessonFeedback(current.session.sessionId, attemptId);
    const session = await apiClient.getLessonSession(current.session.sessionId);
    setState({ status: "content", value: { ...current, session } });
    return completion;
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
      onSubmitAudioAttempt={submitAudioAttempt}
      onConfirmTranscript={confirmTranscript}
      onGetAttempt={(attemptId) => apiClient.getLessonAttempt(sessionId, attemptId)}
      onCompleteFeedback={completeFeedback}
      onRefreshSession={() => updateSession((session) => apiClient.getLessonSession(session.sessionId))}
      onListRolePlayTurns={() => apiClient.listRolePlayTurns(sessionId).then((page) => page.items)}
      onStreamRolePlayMessage={(request, onEvent, idempotencyKey) => apiClient.streamRolePlayMessage(
        sessionId, request, onEvent, { idempotencyKey },
      )}
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
  onSubmitAudioAttempt,
  onConfirmTranscript,
  onGetAttempt,
  onCompleteFeedback,
  onRefreshSession,
  onListRolePlayTurns,
  onStreamRolePlayMessage,
}: {
  value: ScenarioLessonLoadedState;
  onBack: () => void;
  onImageError: () => void;
  onAudioError: () => void;
  onPause: () => void;
  onResume: () => void;
  onCompleteStep: (step: LessonStep) => void;
  onSubmitAttempt: (taskId: string, text: string) => Promise<LessonAttemptReceipt>;
  onSubmitAudioAttempt?: (taskId: string, file: Blob, durationMs: number) => Promise<LessonAttemptReceipt>;
  onConfirmTranscript?: (
    attemptId: string,
    decision: "CONFIRM" | "CORRECT" | "RE_RECORD",
    correctedText?: string,
  ) => Promise<LessonAttemptReceipt>;
  onGetAttempt: (attemptId: string) => Promise<LessonAttemptReceipt>;
  onCompleteFeedback: (attemptId: string) => Promise<LessonFeedbackCompletion>;
  onRefreshSession: () => Promise<void>;
  onListRolePlayTurns: () => Promise<RolePlayTurn[]>;
  onStreamRolePlayMessage: (
    request: RolePlayMessageRequest,
    onEvent: SseEventHandler,
    idempotencyKey: string,
  ) => Promise<void>;
}) {
  const { t } = useI18n();
  const [transcriptVisible, setTranscriptVisible] = useState(false);
  const [advancing, setAdvancing] = useState(false);
  const [attemptText, setAttemptText] = useState("");
  const [attemptSubmitting, setAttemptSubmitting] = useState(false);
  const [attemptError, setAttemptError] = useState<string>();
  const [objectiveResult, setObjectiveResult] = useState<LessonAttemptReceipt["objectiveResult"]>();
  const [feedbackAttempt, setFeedbackAttempt] = useState<LessonAttemptReceipt>();
  const [evidenceSummary, setEvidenceSummary] = useState<LessonFeedbackCompletion>();
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
      setFeedbackAttempt(receipt);
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
            activeQuestion={activeQuestion} onSubmitAttempt={submitCurrentAttempt}
            onSubmitAudioAttempt={onSubmitAudioAttempt} onConfirmTranscript={onConfirmTranscript}
            onListRolePlayTurns={onListRolePlayTurns} onStreamRolePlayMessage={onStreamRolePlayMessage}
            feedbackAttempt={feedbackAttempt} onGetAttempt={onGetAttempt}
            onCompleteFeedback={async (attemptId) => { const summary = await onCompleteFeedback(attemptId); setEvidenceSummary(summary); }}
            evidenceSummary={evidenceSummary} onRolePlayAttempt={setFeedbackAttempt} onRefreshSession={onRefreshSession} />
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
  onSubmitAudioAttempt,
  onConfirmTranscript,
  feedbackAttempt,
  onGetAttempt,
  onCompleteFeedback,
  evidenceSummary,
  onRolePlayAttempt,
  onRefreshSession,
  onListRolePlayTurns,
  onStreamRolePlayMessage,
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
  onSubmitAudioAttempt?: (taskId: string, file: Blob, durationMs: number) => Promise<LessonAttemptReceipt>;
  onConfirmTranscript?: (
    attemptId: string,
    decision: "CONFIRM" | "CORRECT" | "RE_RECORD",
    correctedText?: string,
  ) => Promise<LessonAttemptReceipt>;
  feedbackAttempt?: LessonAttemptReceipt;
  onGetAttempt: (attemptId: string) => Promise<LessonAttemptReceipt>;
  onCompleteFeedback: (attemptId: string) => Promise<void>;
  evidenceSummary?: LessonFeedbackCompletion;
  onRolePlayAttempt: (attempt: LessonAttemptReceipt) => void;
  onRefreshSession: () => Promise<void>;
  onListRolePlayTurns: () => Promise<RolePlayTurn[]>;
  onStreamRolePlayMessage: (
    request: RolePlayMessageRequest,
    onEvent: SseEventHandler,
    idempotencyKey: string,
  ) => Promise<void>;
}) {
  const { t } = useI18n();
  const recorder = useRef<MediaRecorder | null>(null);
  const activeStream = useRef<MediaStream | null>(null);
  const recordingTimeout = useRef<number | null>(null);
  const chunks = useRef<Blob[]>([]);
  const recordingStartedAt = useRef(0);
  const [voiceState, setVoiceState] = useState<"idle" | "recording" | "uploading" | "confirming">("idle");
  const [voiceReceipt, setVoiceReceipt] = useState<LessonAttemptReceipt>();
  const [correctedTranscript, setCorrectedTranscript] = useState("");
  const [voiceError, setVoiceError] = useState<string>();
  const { lesson, session } = value;
  const showListening = session.currentStep === "FIRST_LISTEN";
  const showLanguage = session.currentStep === "TRANSCRIPT_EXPRESSIONS" || showListening || value.audioUnavailable;

  if (session.currentStep === "FEEDBACK") {
    return <FeedbackPanel attempt={feedbackAttempt} evidenceSummary={evidenceSummary}
      onGetAttempt={onGetAttempt} onComplete={onCompleteFeedback} />;
  }

  useEffect(() => () => {
    if (recordingTimeout.current !== null) window.clearTimeout(recordingTimeout.current);
    if (recorder.current?.state === "recording") {
      recorder.current.onstop = null;
      recorder.current.stop();
    }
    activeStream.current?.getTracks().forEach((track) => track.stop());
  }, []);

  async function startRecording() {
    if (!onSubmitAudioAttempt || !navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      setVoiceError(t("lesson.voice.unsupported"));
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      activeStream.current = stream;
      const next = new MediaRecorder(stream, MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
        ? { mimeType: "audio/webm;codecs=opus" } : undefined);
      chunks.current = [];
      next.ondataavailable = (event) => { if (event.data.size > 0) chunks.current.push(event.data); };
      next.onstop = () => {
        if (recordingTimeout.current !== null) window.clearTimeout(recordingTimeout.current);
        const durationMs = Math.max(100, Date.now() - recordingStartedAt.current);
        const blob = new Blob(chunks.current, { type: next.mimeType || "audio/webm" });
        stream.getTracks().forEach((track) => track.stop());
        activeStream.current = null;
        setVoiceState("uploading");
        void onSubmitAudioAttempt(lesson.guidedSpeaking!.taskId, blob, durationMs)
          .then((receipt) => {
            setVoiceReceipt(receipt);
            setCorrectedTranscript(receipt.transcript?.text ?? "");
            setVoiceState("idle");
          })
          .catch((error) => {
            setVoiceError(error instanceof Error ? error.message : t("lesson.attempt.error"));
            setVoiceState("idle");
          });
      };
      recorder.current = next;
      recordingStartedAt.current = Date.now();
      setVoiceError(undefined);
      setVoiceReceipt(undefined);
      setVoiceState("recording");
      next.start();
      recordingTimeout.current = window.setTimeout(() => {
        if (next.state === "recording") next.stop();
      }, 600_000);
    } catch (error) {
      setVoiceError(error instanceof Error ? error.message : t("lesson.attempt.error"));
    }
  }

  function stopRecording() {
    if (recorder.current?.state === "recording") recorder.current.stop();
  }

  async function decideTranscript(decision: "CONFIRM" | "CORRECT" | "RE_RECORD") {
    if (!voiceReceipt || !onConfirmTranscript || voiceState === "confirming") return;
    setVoiceState("confirming");
    setVoiceError(undefined);
    try {
      const receipt = await onConfirmTranscript(
        voiceReceipt.attemptId,
        decision,
        decision === "CORRECT" ? correctedTranscript.trim() : undefined,
      );
      setVoiceReceipt(decision === "RE_RECORD" ? undefined : receipt);
    } catch (error) {
      setVoiceError(error instanceof Error ? error.message : t("lesson.attempt.error"));
    } finally {
      setVoiceState("idle");
    }
  }

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
        {onSubmitAudioAttempt ? (
          <section className="lesson-voice-control" aria-live="polite">
            <strong>{t("lesson.voice.title")}</strong>
            <p>{voiceState === "recording" ? t("lesson.voice.recording")
              : voiceState === "uploading" ? t("lesson.voice.processing") : t("lesson.voice.hint")}</p>
            {voiceState === "recording" ? (
              <button className="voice-record-button is-recording" type="button" onClick={stopRecording}>
                {t("lesson.voice.stop")}
              </button>
            ) : (
              <button className="voice-record-button" type="button" disabled={voiceState !== "idle" || session.status === "PAUSED"}
                onClick={() => void startRecording()}>{t("lesson.voice.start")}</button>
            )}
            {voiceReceipt?.transcript?.confirmationRequired ? (
              <div className="transcript-confirmation" role="alert">
                <strong>{t("lesson.voice.confirmTitle")}</strong>
                <textarea aria-label={t("lesson.voice.transcript")} rows={3} value={correctedTranscript}
                  onChange={(event) => setCorrectedTranscript(event.target.value)} />
                <div>
                  <button type="button" disabled={voiceState !== "idle"} onClick={() => void decideTranscript("CONFIRM")}>
                    {t("lesson.voice.confirm")}
                  </button>
                  <button type="button" disabled={!correctedTranscript.trim() || voiceState !== "idle"}
                    onClick={() => void decideTranscript("CORRECT")}>{t("lesson.voice.correct")}</button>
                  <button type="button" disabled={voiceState !== "idle"} onClick={() => void decideTranscript("RE_RECORD")}>
                    {t("lesson.voice.rerecord")}
                  </button>
                </div>
              </div>
            ) : null}
            {voiceError ? <p className="lesson-attempt-error" role="alert">{voiceError}</p> : null}
          </section>
        ) : null}
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

  if (session.currentStep === "ROLE_PLAY" && lesson.rolePlay) {
    return <RolePlayPanel lesson={lesson.rolePlay} paused={session.status === "PAUSED"}
      onListTurns={onListRolePlayTurns} onStream={onStreamRolePlayMessage}
      onAnalysisReady={async (attemptId) => { onRolePlayAttempt(await onGetAttempt(attemptId)); await onRefreshSession(); }} />;
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

function RolePlayPanel({
  lesson,
  paused,
  onListTurns,
  onStream,
  onAnalysisReady,
}: {
  lesson: NonNullable<ScenarioLessonLoadedState["lesson"]["rolePlay"]>;
  paused: boolean;
  onListTurns: () => Promise<RolePlayTurn[]>;
  onStream: (request: RolePlayMessageRequest, onEvent: SseEventHandler, idempotencyKey: string) => Promise<void>;
  onAnalysisReady: (attemptId: string) => Promise<void>;
}) {
  const { t } = useI18n();
  const [turns, setTurns] = useState<RolePlayTurn[]>([]);
  const [message, setMessage] = useState("");
  const [replyDraft, setReplyDraft] = useState("");
  const [phase, setPhase] = useState<"loading" | "idle" | "streaming" | "reconnecting" | "error">("loading");
  const [error, setError] = useState<string>();
  const pending = useRef<{ request: RolePlayMessageRequest; idempotencyKey: string } | undefined>(undefined);

  const reconcile = useCallback(async () => {
    const values = await onListTurns();
    setTurns(values);
    return values;
  }, [onListTurns]);

  useEffect(() => {
    let active = true;
    void reconcile().then(() => { if (active) setPhase("idle"); })
      .catch((reason) => {
        if (!active) return;
        setError(reason instanceof Error ? reason.message : t("lesson.rolePlay.loadError"));
        setPhase("error");
      });
    return () => { active = false; };
  }, [reconcile, t]);

  async function run(request: RolePlayMessageRequest, idempotencyKey: string) {
    pending.current = { request, idempotencyKey };
    setPhase("streaming");
    setError(undefined);
    setReplyDraft("");
    let streamError: { code: string; retryable: boolean } | undefined;
    try {
      await onStream(request, (event: SseEvent) => {
        if (event.event === "reply.delta") setReplyDraft((current) => current + event.data.text);
        if (event.event === "stream.error") streamError = event.data;
      }, idempotencyKey);
      const values = await reconcile();
      const accepted = values.find((turn) => turn.turnId === request.conversationTurnId);
      if (accepted?.attemptId) await onAnalysisReady(accepted.attemptId);
      if (streamError || (accepted && accepted.status !== "COMPLETED")) {
        const retryable = streamError?.retryable ?? accepted?.status === "FAILED_RETRYABLE";
        setError(t(retryable ? "lesson.rolePlay.retryable" : "lesson.rolePlay.failed"));
        setPhase(retryable ? "reconnecting" : "error");
      } else {
        pending.current = undefined;
        setMessage("");
        setReplyDraft("");
        setPhase("idle");
      }
    } catch (reason) {
      setPhase("reconnecting");
      setError(t("lesson.rolePlay.disconnected"));
      try {
        const values = await reconcile();
        if (values.some((turn) => turn.turnId === request.conversationTurnId && turn.status === "COMPLETED")) {
          pending.current = undefined;
          setMessage("");
          setReplyDraft("");
          setPhase("idle");
          setError(undefined);
        }
      } catch {
        // Keep the reconnect action available; the accepted turn remains server-side.
      }
    }
  }

  function submit() {
    const text = message.trim();
    if (!text || paused || phase === "streaming") return;
    const turnId = globalThis.crypto?.randomUUID?.() ?? `turn-${Date.now()}`;
    void run({ taskId: lesson.taskId, text, conversationTurnId: turnId }, turnId);
  }

  return (
    <section className="role-play-panel" aria-labelledby="scenario-lesson-title">
      <p className="scene-kicker">{t("lesson.step.ROLE_PLAY")}</p>
      <h1 id="scenario-lesson-title">{t("lesson.rolePlay.title")}</h1>
      <p className="scenario-context">{lesson.goal}</p>
      <div className="role-play-boundary">
        <span>{t("lesson.rolePlay.youAre")}: <strong>{lesson.userRole}</strong></span>
        <span>{t("lesson.rolePlay.aiIs")}: <strong>{lesson.aiRole}</strong></span>
      </div>
      <ul className="role-play-criteria">
        {lesson.successCriteria.map((criterion) => <li key={criterion}>{criterion}</li>)}
      </ul>
      <div className="role-play-dialogue" aria-live="polite">
        <div className="role-play-message is-ai"><strong>{lesson.aiRole}</strong><p>{lesson.openingLine}</p></div>
        {turns.map((turn) => (
          <div className="role-play-turn" key={turn.turnId}>
            {turn.learnerText ? <div className="role-play-message is-learner"><strong>{t("lesson.rolePlay.you")}</strong><p>{turn.learnerText}</p></div> : null}
            {turn.replyText ? <div className="role-play-message is-ai"><strong>{lesson.aiRole}</strong><p>{turn.replyText}</p></div> : null}
          </div>
        ))}
        {replyDraft ? <div className="role-play-message is-ai is-streaming"><strong>{lesson.aiRole}</strong><p>{replyDraft}</p></div> : null}
        {phase === "loading" ? <p role="status">{t("lesson.rolePlay.loading")}</p> : null}
      </div>
      {error ? <p className="lesson-attempt-error" role="alert">{error}</p> : null}
      {phase === "reconnecting" && pending.current ? (
        <button type="button" className="secondary-action" onClick={() => void run(
          pending.current!.request, pending.current!.idempotencyKey,
        )}>{t("lesson.rolePlay.reconnect")}</button>
      ) : null}
      <label className="lesson-attempt-field">
        <span>{t("lesson.rolePlay.response")}</span>
        <textarea rows={4} value={message} onChange={(event) => setMessage(event.target.value)}
          disabled={paused || phase === "loading" || phase === "streaming"} />
      </label>
      <button className="scenario-primary-action" type="button"
        disabled={!message.trim() || paused || phase === "loading" || phase === "streaming"}
        onClick={submit}>{phase === "streaming" ? t("lesson.rolePlay.streaming") : t("lesson.rolePlay.send")}</button>
    </section>
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
