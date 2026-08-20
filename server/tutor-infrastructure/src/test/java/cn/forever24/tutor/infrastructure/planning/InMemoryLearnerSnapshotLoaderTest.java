package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InMemoryLearnerSnapshotLoaderTest {

    @Test
    void defaultSnapshotContainsValidDeterministicEvidenceTimes() {
        var snapshot = new InMemoryLearnerSnapshotLoader().load(new UserKey("usr-in-memory"));

        assertEquals(2, snapshot.skillStates().size());
        snapshot.skillStates().forEach(state -> assertNotNull(state.lastEvidenceAt()));
    }
}
