package cn.forever24.tutor.application.roleplay;

import java.util.Map;

public record RolePlayStreamEvent(long id, RolePlayStreamEventType type, Map<String, Object> data) {
    public RolePlayStreamEvent {
        if (id < 1 || type == null) throw new IllegalArgumentException("event id and type are required");
        data = Map.copyOf(data == null ? Map.of() : data);
    }
}
