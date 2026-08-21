package cn.forever24.tutor.application.roleplay;

import cn.forever24.tutor.training.RolePlayTurn;

public record RolePlayTurnStoreRecord(String idempotencyKey, String requestHash, RolePlayTurn turn) {
}
