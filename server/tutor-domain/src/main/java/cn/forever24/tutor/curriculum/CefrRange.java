package cn.forever24.tutor.curriculum;

public record CefrRange(CefrLevel minimum, CefrLevel maximum) {

    public CefrRange {
        if (minimum == null || maximum == null) {
            throw new IllegalArgumentException("CEFR range is required");
        }
        if (minimum.ordinal() > maximum.ordinal()) {
            throw new IllegalArgumentException("minimum CEFR level must not exceed maximum");
        }
    }

    public boolean includes(CefrLevel level) {
        return level != null && level.ordinal() >= minimum.ordinal() && level.ordinal() <= maximum.ordinal();
    }
}
