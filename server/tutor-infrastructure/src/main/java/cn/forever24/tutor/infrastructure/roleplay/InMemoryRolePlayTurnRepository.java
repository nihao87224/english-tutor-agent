package cn.forever24.tutor.infrastructure.roleplay;

import cn.forever24.tutor.application.roleplay.RolePlayTurnRepository;
import cn.forever24.tutor.application.roleplay.RolePlayTurnStoreRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.RolePlayTurn;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRolePlayTurnRepository implements RolePlayTurnRepository {
    private final Map<String, OwnedTurn> turns = new ConcurrentHashMap<>();
    private final Map<String, RolePlayTurnStoreRecord> idempotency = new ConcurrentHashMap<>();

    @Override
    public Optional<RolePlayTurnStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String key) {
        return Optional.ofNullable(idempotency.get(scope(userKey, sessionId, key)));
    }

    @Override
    public Optional<RolePlayTurnStoreRecord> findByTurnId(UserKey userKey, String sessionId, String turnId) {
        OwnedTurn value = turns.get(scope(userKey, sessionId, turnId));
        return value == null ? Optional.empty()
                : Optional.of(new RolePlayTurnStoreRecord(value.idempotencyKey(), value.requestHash(), value.turn()));
    }

    @Override
    public List<RolePlayTurn> findBySession(UserKey userKey, String sessionId) {
        String prefix = userKey.value() + "|" + sessionId + "|";
        return turns.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .map(entry -> entry.getValue().turn())
                .sorted(Comparator.comparing(RolePlayTurn::acceptedAt).thenComparing(RolePlayTurn::turnId)).toList();
    }

    @Override
    public synchronized void insert(
            UserKey userKey, RolePlayTurn turn, String idempotencyKey, String requestHash
    ) {
        String turnScope = scope(userKey, turn.sessionId(), turn.turnId());
        String idemScope = scope(userKey, turn.sessionId(), idempotencyKey);
        if (turns.containsKey(turnScope) || idempotency.containsKey(idemScope)) {
            throw new IllegalStateException("role-play turn already exists");
        }
        turns.put(turnScope, new OwnedTurn(turn, idempotencyKey, requestHash));
        idempotency.put(idemScope, new RolePlayTurnStoreRecord(idempotencyKey, requestHash, turn));
    }

    @Override
    public synchronized RolePlayTurn save(UserKey userKey, RolePlayTurn turn, long expectedVersion) {
        String turnScope = scope(userKey, turn.sessionId(), turn.turnId());
        OwnedTurn current = turns.get(turnScope);
        if (current == null || current.turn().version() != expectedVersion) {
            throw new IllegalStateException("role-play turn version conflict");
        }
        OwnedTurn updated = new OwnedTurn(turn, current.idempotencyKey(), current.requestHash());
        turns.put(turnScope, updated);
        idempotency.put(scope(userKey, turn.sessionId(), current.idempotencyKey()),
                new RolePlayTurnStoreRecord(current.idempotencyKey(), current.requestHash(), turn));
        return turn;
    }

    private static String scope(UserKey userKey, String sessionId, String value) {
        return userKey.value() + "|" + sessionId + "|" + value;
    }

    private record OwnedTurn(RolePlayTurn turn, String idempotencyKey, String requestHash) { }
}
