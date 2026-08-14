package cn.forever24.tutor.infrastructure.quota;

import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.DailyQuotaStatus;
import cn.forever24.tutor.application.quota.QuotaException;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryDailyQuotaRepositoryTest {

    private final DailyQuotaApplicationService service = new DailyQuotaApplicationService(
            new InMemoryDailyQuotaRepository(),
            Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC),
            1,
            ZoneId.of("Asia/Shanghai"));

    @Test
    void allowsExactlyOneConcurrentReservationWhenRemainingQuotaIsOne() throws Exception {
        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            int requestIndex = index;
            calls.add(() -> {
                try {
                    QuotaReservation reservation = service.reserve(
                            "user-1",
                            QuotaRequestType.CONVERSATION_REPLY,
                            "idem-" + requestIndex);
                    service.commit(reservation);
                    return true;
                } catch (QuotaException exception) {
                    return false;
                }
            });
        }

        try (var executor = Executors.newFixedThreadPool(20)) {
            long successCount = executor.invokeAll(calls).stream().filter(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).count();

            assertEquals(1, successCount);
        }
        DailyQuotaStatus status = service.currentQuota("user-1");
        assertEquals(1, status.used());
        assertEquals(0, status.remaining());
    }

    @Test
    void repeatedIdempotencyKeyDoesNotDoubleConsumeQuota() {
        QuotaReservation first = service.reserve("user-1", QuotaRequestType.CONVERSATION_REPLY, "same-key");
        service.commit(first);

        QuotaReservation repeated = service.reserve("user-1", QuotaRequestType.CONVERSATION_REPLY, "same-key");
        service.commit(repeated);

        DailyQuotaStatus status = service.currentQuota("user-1");
        assertEquals(1, status.used());
        assertEquals(0, status.remaining());
        assertThrows(QuotaException.class,
                () -> service.reserve("user-1", QuotaRequestType.CONVERSATION_REPLY, "other-key"));
    }

    @Test
    void refundRestoresReservedQuotaBeforeCommit() {
        QuotaReservation reservation = service.reserve("user-1", QuotaRequestType.CONVERSATION_REPLY, "will-fail");

        service.refund(reservation);

        DailyQuotaStatus status = service.currentQuota("user-1");
        assertEquals(0, status.used());
        assertEquals(1, status.remaining());
    }
}
