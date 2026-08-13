package cn.forever24.tutor.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrivacySettingsTest {

    @Test
    void mapsSaveFlagsToRetentionModes() {
        PrivacySettings settings = PrivacySettings.fromContractValues(true, false, 14);

        assertEquals(RawContentRetention.STORE, settings.rawTextRetention());
        assertEquals(RawContentRetention.PROCESS_ONLY, settings.rawAudioRetention());
        assertFalse(settings.saveRawAudio());
    }

    @Test
    void defaultsRawAudioRetentionDaysToZero() {
        PrivacySettings settings = PrivacySettings.fromContractValues(true, true, null);

        assertEquals(0, settings.rawAudioRetentionDays());
    }

    @Test
    void rejectsOutOfRangeRetentionDays() {
        assertThrows(IllegalArgumentException.class,
                () -> PrivacySettings.fromContractValues(true, true, -1));
        assertThrows(IllegalArgumentException.class,
                () -> PrivacySettings.fromContractValues(true, true, 366));
    }
}
