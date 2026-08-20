package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.LearnerPlanningSnapshot;
import cn.forever24.tutor.application.planning.LearnerSnapshotLoader;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLearnerSnapshotLoader implements LearnerSnapshotLoader {

    private final Map<String, LearnerPlanningSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public LearnerPlanningSnapshot load(UserKey userKey) {
        return snapshots.computeIfAbsent(userKey.value(), ignored -> defaultSnapshot(userKey));
    }

    public void put(LearnerPlanningSnapshot snapshot) {
        snapshots.put(snapshot.userKey().value(), snapshot);
    }

    private static LearnerPlanningSnapshot defaultSnapshot(UserKey userKey) {
        return new LearnerPlanningSnapshot(
                userKey,
                PrimaryGoal.GENERAL,
                ZoneId.of("Asia/Shanghai"),
                20,
                1,
                CefrLevel.A2,
                List.of(
                        new PrescriptionSkillState(
                                "speaking", new BigDecimal("0.35"), new BigDecimal("0.65"), CefrLevel.A2, 2, Instant.EPOCH),
                        new PrescriptionSkillState(
                                "listening", new BigDecimal("0.50"), new BigDecimal("0.60"), CefrLevel.A2, 3, Instant.EPOCH)));
    }
}
