package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.ConfirmTranscriptCommand;
import cn.forever24.tutor.application.training.TranscriptConfirmationDecision;

public record TranscriptConfirmationRequest(
        TranscriptConfirmationDecision decision,
        String correctedText
) {
    ConfirmTranscriptCommand toCommand() { return new ConfirmTranscriptCommand(decision, correctedText); }
}
