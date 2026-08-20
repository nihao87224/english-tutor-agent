package cn.forever24.tutor.experience;

import java.util.Optional;

public record ExperienceResolution(
        ExperienceResolutionStatus status,
        Optional<EpisodeMapping> mapping,
        int learnerFitScore,
        int storyContinuityScore,
        String reasonCode
) {
    public ExperienceResolution {
        if (status == null || mapping == null || reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("resolution status, mapping and reason are required");
        }
        if (status == ExperienceResolutionStatus.NO_MAPPING && mapping.isPresent()) {
            throw new IllegalArgumentException("NO_MAPPING result cannot contain a mapping");
        }
        if (status != ExperienceResolutionStatus.NO_MAPPING && mapping.isEmpty()) {
            throw new IllegalArgumentException("matched result requires a mapping");
        }
    }

    public static ExperienceResolution noMapping(String reasonCode) {
        return new ExperienceResolution(
                ExperienceResolutionStatus.NO_MAPPING,
                Optional.empty(),
                0,
                0,
                reasonCode);
    }
}
