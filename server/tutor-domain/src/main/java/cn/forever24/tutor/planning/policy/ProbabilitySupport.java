package cn.forever24.tutor.planning.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ProbabilitySupport {

    static final BigDecimal ZERO = new BigDecimal("0.0000");
    static final BigDecimal ONE = new BigDecimal("1.0000");

    private ProbabilitySupport() {
    }

    static BigDecimal require(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    static BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return ONE;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
