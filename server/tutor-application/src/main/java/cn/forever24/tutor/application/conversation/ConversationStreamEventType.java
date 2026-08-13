package cn.forever24.tutor.application.conversation;

public enum ConversationStreamEventType {
    STATUS("status"),
    TEXT_DELTA("text_delta"),
    CORRECTION_READY("correction_ready"),
    DONE("done");

    private final String eventName;

    ConversationStreamEventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
