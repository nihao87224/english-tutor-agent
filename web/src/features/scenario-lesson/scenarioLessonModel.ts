import type { CatalogAsset, LearningResourceDetail, LessonSession, LessonStep } from "../../shared/api";

export interface LessonSentence {
  sentenceId: string;
  speaker: string;
  text: string;
}

export interface LessonExpression {
  expression: string;
  meaningZh: string;
  usage: string;
}

export interface ScenarioLessonViewModel {
  resourceId: string;
  resourceVersion: string;
  title: string;
  level: string;
  topic: string;
  scene: string;
  communicationGoal: string;
  character: "Lin Muen";
  seasonId: string;
  episodeId: string;
  story: { title: string; context: string; mission: string };
  transcript: LessonSentence[];
  expressions: LessonExpression[];
  taskHero: {
    assetId: string;
    altText: string;
    aspectRatio: string;
    focalPoint: { x: number; y: number };
  };
  audioAssetId?: string;
}

export interface ScenarioLessonLoadedState {
  session: LessonSession;
  lesson: ScenarioLessonViewModel;
  imageUrl?: string;
  audioUrl?: string;
  imageUnavailable: boolean;
  audioUnavailable: boolean;
}

export class ScenarioLessonContentError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ScenarioLessonContentError";
  }
}

export function buildScenarioLesson(resource: LearningResourceDetail): ScenarioLessonViewModel {
  const lessonPackage = objectField(resource.content, "lessonPackage");
  if (stringField(lessonPackage, "character") !== "Lin Muen") {
    throw new ScenarioLessonContentError("Scenario lesson character must be Lin Muen");
  }

  const taskHero = validateTaskHero(resource.taskHero);
  const story = objectField(lessonPackage, "story");
  const transcript = objectField(lessonPackage, "transcript");
  const audio = resource.audioAssets.find((asset) =>
    asset.mediaType === "AUDIO" && normalized(asset.purpose) === "scene_dialogue");

  return {
    resourceId: resource.resourceId,
    resourceVersion: resource.resourceVersion,
    title: resource.title,
    level: resource.level,
    topic: resource.topic,
    scene: resource.scene,
    communicationGoal: resource.communicationGoal,
    character: "Lin Muen",
    seasonId: stringField(lessonPackage, "seasonId"),
    episodeId: stringField(lessonPackage, "episodeId"),
    story: {
      title: stringField(story, "title"),
      context: stringField(story, "context"),
      mission: stringField(story, "mission"),
    },
    transcript: arrayField(transcript.sentences, parseSentence, "transcript.sentences"),
    expressions: arrayField(lessonPackage.expressions, parseExpression, "expressions"),
    taskHero,
    audioAssetId: audio?.assetId,
  };
}

export function lessonLoaded(
  session: LessonSession,
  resource: LearningResourceDetail,
  media: { imageUrl?: string; audioUrl?: string },
): ScenarioLessonLoadedState {
  if (session.resource.resourceId !== resource.resourceId || session.resource.resourceVersion !== resource.resourceVersion) {
    throw new ScenarioLessonContentError("Loaded resource does not match the session-locked version");
  }
  const lesson = buildScenarioLesson(resource);
  return {
    session,
    lesson,
    imageUrl: media.imageUrl,
    audioUrl: media.audioUrl,
    imageUnavailable: !media.imageUrl,
    audioUnavailable: !lesson.audioAssetId || !media.audioUrl,
  };
}

export function withMediaFailure(
  state: ScenarioLessonLoadedState,
  mediaType: "image" | "audio",
): ScenarioLessonLoadedState {
  return mediaType === "image"
    ? { ...state, imageUrl: undefined, imageUnavailable: true }
    : { ...state, audioUrl: undefined, audioUnavailable: true };
}

export function taskHeroStyle(hero: ScenarioLessonViewModel["taskHero"]): {
  aspectRatio: string;
  objectPosition: string;
} {
  return {
    aspectRatio: hero.aspectRatio.replace(":", " / "),
    objectPosition: `${Math.round(hero.focalPoint.x * 100)}% ${Math.round(hero.focalPoint.y * 100)}%`,
  };
}

export function isSpeakingStep(step: LessonStep): boolean {
  return step === "GUIDED_SPEAKING" || step === "ROLE_PLAY" || step === "RETRY";
}

function validateTaskHero(asset?: CatalogAsset | null): ScenarioLessonViewModel["taskHero"] {
  if (!asset || asset.mediaType !== "IMAGE" || normalized(asset.purpose) !== "task_hero") {
    throw new ScenarioLessonContentError("A task_hero image is required");
  }
  const shotType = metadataString(asset, "shotType");
  const displaySurfaces = metadataStrings(asset, "displaySurfaces").map(normalized);
  const altText = metadataString(asset, "altText");
  const environmentalShot = shotType.startsWith("environmental_");
  const trainingSurface = displaySurfaces.includes("scenario_intro") && displaySurfaces.includes("scenario_training");
  if (!environmentalShot || !trainingSurface || !/lin muen/i.test(altText)) {
    throw new ScenarioLessonContentError("Avatar-only or non-scenario task heroes are not allowed");
  }
  const focalPoint = objectField(asset.metadata, "focalPoint");
  const x = numberField(focalPoint, "x");
  const y = numberField(focalPoint, "y");
  if (x < 0 || x > 1 || y < 0 || y > 1) {
    throw new ScenarioLessonContentError("Task hero focalPoint must stay between 0 and 1");
  }
  return {
    assetId: asset.assetId,
    altText,
    aspectRatio: metadataString(asset, "aspectRatio"),
    focalPoint: { x, y },
  };
}

function parseSentence(value: unknown): LessonSentence {
  const sentence = asObject(value, "transcript sentence");
  return {
    sentenceId: stringField(sentence, "sentenceId"),
    speaker: stringField(sentence, "speaker"),
    text: stringField(sentence, "text"),
  };
}

function parseExpression(value: unknown): LessonExpression {
  const expression = asObject(value, "expression");
  return {
    expression: stringField(expression, "expression"),
    meaningZh: stringField(expression, "meaningZh"),
    usage: stringField(expression, "usage"),
  };
}

function metadataString(asset: CatalogAsset, key: string): string {
  return stringField(asset.metadata, key);
}

function metadataStrings(asset: CatalogAsset, key: string): string[] {
  const value = asset.metadata[key];
  if (!Array.isArray(value) || value.some((item) => typeof item !== "string")) {
    throw new ScenarioLessonContentError(`${key} must be a string array`);
  }
  return value as string[];
}

function normalized(value: string): string {
  return value.trim().toLowerCase();
}

function objectField(object: Record<string, unknown>, key: string): Record<string, unknown> {
  return asObject(object[key], key);
}

function asObject(value: unknown, field: string): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new ScenarioLessonContentError(`${field} must be an object`);
  }
  return value as Record<string, unknown>;
}

function stringField(object: Record<string, unknown>, key: string): string {
  const value = object[key];
  if (typeof value !== "string" || !value.trim()) {
    throw new ScenarioLessonContentError(`${key} must be a non-empty string`);
  }
  return value;
}

function numberField(object: Record<string, unknown>, key: string): number {
  const value = object[key];
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new ScenarioLessonContentError(`${key} must be a number`);
  }
  return value;
}

function arrayField<T>(value: unknown, parser: (item: unknown) => T, field: string): T[] {
  if (!Array.isArray(value)) {
    throw new ScenarioLessonContentError(`${field} must be an array`);
  }
  return value.map(parser);
}
