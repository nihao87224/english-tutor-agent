package cn.forever24.tutor.application.roleplay;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.RolePlayTurn;

import java.util.List;
import java.util.Optional;

public interface RolePlayTurnRepository {
    Optional<RolePlayTurnStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String idempotencyKey);
    Optional<RolePlayTurnStoreRecord> findByTurnId(UserKey userKey, String sessionId, String turnId);
    List<RolePlayTurn> findBySession(UserKey userKey, String sessionId);
    void insert(UserKey userKey, RolePlayTurn turn, String idempotencyKey, String requestHash);
    RolePlayTurn save(UserKey userKey, RolePlayTurn turn, long expectedVersion);
}
