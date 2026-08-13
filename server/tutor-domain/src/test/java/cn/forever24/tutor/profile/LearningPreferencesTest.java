package cn.forever24.tutor.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningPreferencesTest {

    @Test
    void acceptsContractPreferenceValues() {
        LearningPreferences preferences = LearningPreferences.fromContractValues(
                30,
                "LIGHT",
                true,
                false,
                true);

        assertEquals(30, preferences.dailyMinutes());
        assertEquals(CorrectionStyle.LIGHT, preferences.correctionStyle());
        assertFalse(preferences.saveRawText());
    }

    @Test
    void rejectsUnsupportedDailyMinutesAndCorrectionStyle() {
        assertThrows(IllegalArgumentException.class,
                () -> LearningPreferences.fromContractValues(17, "LIGHT", true, true, true));

        assertThrows(IllegalArgumentException.class,
                () -> LearningPreferences.fromContractValues(20, "VERBOSE", true, true, true));
    }

    @Test
    void rejectsMissingBooleanFields() {
        assertThrows(IllegalArgumentException.class,
                () -> LearningPreferences.fromContractValues(20, "STANDARD", null, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> LearningPreferences.fromContractValues(20, "STANDARD", false, null, true));
    }
}
