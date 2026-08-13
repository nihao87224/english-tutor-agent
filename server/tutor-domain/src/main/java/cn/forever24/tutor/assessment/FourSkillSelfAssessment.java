package cn.forever24.tutor.assessment;

public record FourSkillSelfAssessment(
        SelfRating listening,
        SelfRating speaking,
        SelfRating reading,
        SelfRating writing
) {

    public FourSkillSelfAssessment {
        if (listening == null) {
            throw new IllegalArgumentException("listening rating is required");
        }
        if (speaking == null) {
            throw new IllegalArgumentException("speaking rating is required");
        }
        if (reading == null) {
            throw new IllegalArgumentException("reading rating is required");
        }
        if (writing == null) {
            throw new IllegalArgumentException("writing rating is required");
        }
    }

    public static FourSkillSelfAssessment fromContractValues(
            String listening,
            String speaking,
            String reading,
            String writing
    ) {
        return new FourSkillSelfAssessment(
                SelfRating.fromContractValue(listening, "listening"),
                SelfRating.fromContractValue(speaking, "speaking"),
                SelfRating.fromContractValue(reading, "reading"),
                SelfRating.fromContractValue(writing, "writing"));
    }

    public SelfRating estimatedBand() {
        int total = listening.score() + speaking.score() + reading.score() + writing.score();
        int roundedAverage = Math.round(total / 4.0f);
        for (SelfRating rating : SelfRating.values()) {
            if (rating.score() == roundedAverage) {
                return rating;
            }
        }
        return SelfRating.INTERMEDIATE;
    }
}
