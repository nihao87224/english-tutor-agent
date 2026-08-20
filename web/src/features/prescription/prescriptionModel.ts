import { ApiError, type DailyLearningPrescription, type PrescriptionRegenerationReason, type PrescriptionTaskHero } from "../../shared/api";

export type PrescriptionViewState =
  | { status: "loading" }
  | { status: "content"; prescription: DailyLearningPrescription; regenerating: boolean; notice?: string }
  | { status: "empty"; message?: string }
  | { status: "error"; message: string }
  | { status: "stale"; prescription: DailyLearningPrescription }
  | { status: "fallback"; message: string };

export function prescriptionLoaded(prescription: DailyLearningPrescription): PrescriptionViewState {
  return prescription.blocks.some((block) => block.status === "READY")
    ? { status: "content", prescription, regenerating: false }
    : { status: "empty" };
}

export function prescriptionFailed(
  error: unknown,
  previous?: DailyLearningPrescription,
): PrescriptionViewState {
  if (error instanceof ApiError && error.problem?.code === "PRESCRIPTION_STALE" && previous) {
    return { status: "stale", prescription: previous };
  }
  if (error instanceof ApiError && error.problem?.code === "PRESCRIPTION_NO_CANDIDATE") {
    return error.problem.fallbackAvailable
      ? { status: "fallback", message: error.message }
      : { status: "empty", message: error.message };
  }
  return { status: "error", message: error instanceof Error ? error.message : "Prescription request failed" };
}

export function primaryPrescriptionBlock(prescription: DailyLearningPrescription) {
  const readyBlocks = prescription.blocks.filter((block) => block.status === "READY");
  return readyBlocks.find((block) => block.type === "OUTPUT") ?? readyBlocks[0];
}

export function taskHeroStyle(hero: PrescriptionTaskHero): { objectPosition: string; aspectRatio: string } {
  const x = clampFocalPoint(hero.focalPoint.x);
  const y = clampFocalPoint(hero.focalPoint.y);
  return {
    objectPosition: `${formatPercent(x)}% ${formatPercent(y)}%`,
    aspectRatio: normalizeAspectRatio(hero.aspectRatio),
  };
}

export function buildRegenerationRequest(
  prescription: DailyLearningPrescription,
  reason: PrescriptionRegenerationReason,
  availableMinutes?: number,
) {
  return {
    currentPrescriptionId: prescription.prescriptionId,
    currentVersion: prescription.version,
    reason,
    availableMinutes: reason === "TIME_INSUFFICIENT" ? availableMinutes : undefined,
  };
}

export function createPrescriptionIdempotencyKey(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? `prescription-${crypto.randomUUID()}`
    : `prescription-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function clampFocalPoint(value: number): number {
  if (!Number.isFinite(value)) return 0.5;
  return Math.min(1, Math.max(0, value));
}

function formatPercent(value: number): string {
  return Number((value * 100).toFixed(2)).toString();
}

function normalizeAspectRatio(value: string): string {
  const match = /^\s*(\d+(?:\.\d+)?)\s*:\s*(\d+(?:\.\d+)?)\s*$/.exec(value);
  return match ? `${match[1]} / ${match[2]}` : "16 / 9";
}
