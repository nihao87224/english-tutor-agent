package cn.forever24.tutor.api.roleplay;

import java.util.List;

public record RolePlayTurnPageResponse(List<RolePlayTurnResponse> items) {
    public RolePlayTurnPageResponse {
        items = List.copyOf(items);
    }
}
