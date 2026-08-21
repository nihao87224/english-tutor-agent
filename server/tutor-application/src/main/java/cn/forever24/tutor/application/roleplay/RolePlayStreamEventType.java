package cn.forever24.tutor.application.roleplay;

public enum RolePlayStreamEventType {
    TURN_ACCEPTED("turn.accepted"),
    REPLY_DELTA("reply.delta"),
    REPLY_COMPLETED("reply.completed"),
    ANALYSIS_PENDING("analysis.pending"),
    STREAM_ERROR("stream.error");

    private final String eventName;
    RolePlayStreamEventType(String eventName) { this.eventName = eventName; }
    public String eventName() { return eventName; }
}
