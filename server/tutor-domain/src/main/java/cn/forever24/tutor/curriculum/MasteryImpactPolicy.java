package cn.forever24.tutor.curriculum;

public record MasteryImpactPolicy(boolean validatedEvidenceRequired, boolean completionUpdatesMastery) {

    public MasteryImpactPolicy {
        if (!validatedEvidenceRequired) {
            throw new IllegalArgumentException("validated evidence must be required for mastery updates");
        }
        if (completionUpdatesMastery) {
            throw new IllegalArgumentException("lesson completion cannot update mastery directly");
        }
    }
}
