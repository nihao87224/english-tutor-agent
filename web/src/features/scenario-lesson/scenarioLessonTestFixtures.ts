import type { LearningResourceDetail, LessonSession, LessonStep } from "../../shared/api";

export function fixtureLessonSession(step: LessonStep = "SCENE_CONTEXT"): LessonSession {
  return {
    sessionId: "lesson-session-ep006",
    prescriptionId: "prx-airport",
    prescriptionVersion: 3,
    blockId: "output",
    status: "IN_PROGRESS",
    resource: { resourceId: "season1.ep006.gate_change.b1", resourceVersion: "1.0.0" },
    skillUnitVariantId: "travel.confirm_gate_change.b1",
    episodeMappingId: "season1.ep006.gate_change.travel.confirm_gate_change.b1",
    inputMode: "VOICE_OR_TEXT",
    currentStep: step,
    step: {
      stepId: step,
      completionMode: ["SCENE_CONTEXT", "FIRST_LISTEN", "TRANSCRIPT_EXPRESSIONS"].includes(step)
        ? "CLIENT_ACKNOWLEDGEMENT"
        : "ATTEMPT_REQUIRED",
      clientCompletable: ["SCENE_CONTEXT", "FIRST_LISTEN", "TRANSCRIPT_EXPRESSIONS"].includes(step),
    },
    progress: { completedSteps: step === "SCENE_CONTEXT" ? 0 : 1, totalRequiredSteps: 9 },
    attemptProgress: step === "COMPREHENSION" ? {
      stepId: "COMPREHENSION",
      completedTaskIds: [],
      remainingTaskIds: ["gate-q1", "gate-q2"],
      nextStepEligible: false,
    } : null,
    version: 2,
  };
}

export function fixtureLearningResource(): LearningResourceDetail {
  const transcript = [
    { sentenceId: "gate-001", speaker: "Lin Muen", text: "Could you help me check?" },
    { sentenceId: "gate-002", speaker: "Airport Agent", text: "Your flight now departs from Gate 24." },
  ];
  return {
    resourceId: "season1.ep006.gate_change.b1",
    resourceVersion: "1.0.0",
    collectionId: "INTERNAL_SCENARIO_LIBRARY",
    providerCode: "english-tutor-agent",
    type: "SCENARIO_LESSON",
    title: "Confirm a Gate Change with Lin Muen",
    description: "Airport scenario lesson",
    language: "en",
    level: "B1",
    topic: "Travel",
    scene: "GATE_CHANGE",
    communicationGoal: "Confirm a changed boarding gate and the next action clearly",
    accessScope: "PUBLIC",
    publishStatus: "PUBLISHED",
    estimatedMinutes: 12,
    skillUnitVariantIds: ["travel.confirm_gate_change.b1"],
    taskHero: {
      assetId: "season1.ep006.gate_change.b1.task-hero",
      assetVersion: "1.0.0",
      mediaType: "IMAGE",
      purpose: "TASK_HERO",
      accessScope: "PUBLIC",
      mimeType: "image/webp",
      contentHash: "sha256:hero",
      byteLength: 1234,
      metadata: {
        aspectRatio: "16:9",
        shotType: "environmental_full_body",
        displaySurfaces: ["prescription_card", "scenario_intro", "scenario_training"],
        focalPoint: { x: 0.62, y: 0.48 },
        altText: "Lin Muen stands full body near an airport boarding gate and checks changed flight details.",
      },
    },
    audioAssets: [{
      assetId: "season1.ep006.gate_change.b1.scene-dialogue",
      assetVersion: "1.0.0",
      mediaType: "AUDIO",
      purpose: "SCENE_DIALOGUE",
      accessScope: "PUBLIC",
      mimeType: "audio/mpeg",
      contentHash: "sha256:audio",
      byteLength: 2345,
      metadata: { transcriptRef: "ep006-transcript" },
    }],
    learnerFit: {},
    content: {
      lessonPackage: {
        character: "Lin Muen",
        seasonId: "S01",
        episodeId: "EP006",
        story: {
          title: "The Gate Has Changed",
          context: "Lin Muen is waiting in the airport departure area when she sees that her gate may have changed.",
          mission: "Help Lin Muen confirm the new gate and find out when boarding starts.",
        },
        transcript: { transcriptId: "ep006-transcript", sentences: transcript },
        expressions: [{ expression: "Could you help me check?", meaningZh: "你能帮我确认一下吗？", usage: "Polite confirmation request" }],
        questions: [
          { questionId: "gate-q1", prompt: "Which gate should Lin Muen use?", answer: "Gate 24" },
          { questionId: "gate-q2", prompt: "When does boarding begin?", answer: "At 3:20" },
        ],
        practice: [{
          taskId: "gate-guided-1",
          type: "guided_speaking",
          prompt: "Tell Lin Muen the new gate and boarding time in one clear response.",
          successCriteria: ["State Gate 24", "State 3:20"],
          scaffolding: ["Your flight leaves from...", "Boarding begins at..."],
        }],
        rolePlay: {
          taskId: "gate-roleplay-1",
          goal: "Confirm the changed gate and boarding time with the airport agent.",
          userRole: "Traveler helping Lin Muen",
          aiRole: "Airport agent",
          successCriteria: ["Confirm Gate 24", "Confirm boarding at 3:20"],
          openingLine: "Good afternoon. How can I help you and Lin Muen?",
        },
      },
    },
    assets: [],
    publishedAt: "2026-08-20T00:00:00Z",
  };
}
