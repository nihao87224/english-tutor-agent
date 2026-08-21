package cn.forever24.tutor.application.roleplay;

import java.util.List;

public record RolePlayStreamResult(List<RolePlayStreamEvent> events, boolean replayed) {
    public RolePlayStreamResult {
        events = List.copyOf(events);
    }
}
