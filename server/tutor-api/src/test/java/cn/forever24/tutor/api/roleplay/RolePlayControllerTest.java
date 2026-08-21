package cn.forever24.tutor.api.roleplay;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.roleplay.*;
import cn.forever24.tutor.training.RolePlayTurn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RolePlayControllerTest {
    @Test
    void resolvesCurrentActorForStreamAndReconcileEndpoints() {
        RolePlayApplicationService service = mock(RolePlayApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        RolePlayController controller = new RolePlayController(service, resolver, new ObjectMapper());
        RolePlayMessageRequest request = new RolePlayMessageRequest(
                "gate-role", "Gate 24?", null, "turn-1");
        when(service.stream("usr-1", "lsn-1", request.toCommand(), "idem-1"))
                .thenReturn(new RolePlayStreamResult(List.of(
                        new RolePlayStreamEvent(1, RolePlayStreamEventType.TURN_ACCEPTED,
                                Map.of("attemptId", "att-1", "turnId", "turn-1", "replayed", false))), false));

        assertNotNull(controller.stream("lsn-1", "idem-1", request));
        verify(service).stream("usr-1", "lsn-1", request.toCommand(), "idem-1");

        RolePlayTurn turn = RolePlayTurn.accepted(
                "turn-1", "lsn-1", "att-1", "gate-role", "Gate 24?", false,
                Instant.parse("2026-08-21T01:00:00Z"));
        when(service.listTurns("usr-1", "lsn-1")).thenReturn(List.of(turn));
        RolePlayTurnPageResponse page = controller.list("lsn-1");
        assertEquals("turn-1", page.items().getFirst().turnId());
        assertEquals("ACCEPTED", page.items().getFirst().status());
    }

    @Test
    void validatesExactlyOneInputBeforeCallingApplicationService() {
        assertThrows(IllegalArgumentException.class,
                () -> new RolePlayMessageRequest("gate-role", "Hello", "audio-1", "turn-1").toCommand());
    }
}
