package cn.forever24.tutor.application.quota;

import java.time.LocalDate;

public record QuotaReservation(
        String id,
        String userKey,
        LocalDate quotaDate,
        QuotaRequestType requestType,
        String idempotencyKey,
        QuotaReservationStatus status
) {
}
