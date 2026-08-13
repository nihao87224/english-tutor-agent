package cn.forever24.tutor.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrimaryGoalTest {

    @Test
    void acceptsContractGoalValues() {
        assertEquals(PrimaryGoal.WORKPLACE, PrimaryGoal.fromContractValue("WORKPLACE"));
        assertEquals(PrimaryGoal.GENERAL, PrimaryGoal.fromContractValue("GENERAL"));
        assertEquals(PrimaryGoal.IELTS, PrimaryGoal.fromContractValue("IELTS"));
    }

    @Test
    void rejectsMissingOrUnsupportedGoalValues() {
        assertThrows(IllegalArgumentException.class, () -> PrimaryGoal.fromContractValue(null));
        assertThrows(IllegalArgumentException.class, () -> PrimaryGoal.fromContractValue(""));
        assertThrows(IllegalArgumentException.class, () -> PrimaryGoal.fromContractValue("TRAVEL"));
    }
}
