package cn.forever24.tutor.planning;

import java.math.BigDecimal;

public record PrescriptionTaskHero(
        String assetKey,
        String publicUrl,
        String aspectRatio,
        BigDecimal focalPointX,
        BigDecimal focalPointY,
        String altText
) {

    public PrescriptionTaskHero {
        assetKey = required(assetKey, "assetKey", 180);
        if (publicUrl != null && publicUrl.isBlank()) {
            throw new IllegalArgumentException("publicUrl must not be blank");
        }
        aspectRatio = required(aspectRatio, "aspectRatio", 16);
        focalPointX = coordinate(focalPointX, "focalPointX");
        focalPointY = coordinate(focalPointY, "focalPointY");
        altText = required(altText, "altText", 500);
    }

    private static BigDecimal coordinate(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
        return value;
    }

    private static String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.strip().length() > maximumLength) {
            throw new IllegalArgumentException("valid task hero " + field + " is required");
        }
        return value.strip();
    }
}
