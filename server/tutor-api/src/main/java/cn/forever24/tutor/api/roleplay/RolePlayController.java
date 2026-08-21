package cn.forever24.tutor.api.roleplay;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.roleplay.RolePlayApplicationService;
import cn.forever24.tutor.application.roleplay.RolePlayStreamEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/lesson-sessions/{sessionId}/role-play")
public class RolePlayController {
    private final RolePlayApplicationService service;
    private final CurrentUserKeyResolver currentUserKeyResolver;
    private final ObjectMapper objectMapper;

    public RolePlayController(
            RolePlayApplicationService service,
            CurrentUserKeyResolver currentUserKeyResolver,
            ObjectMapper objectMapper
    ) {
        this.service = service;
        this.currentUserKeyResolver = currentUserKeyResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody RolePlayMessageRequest request
    ) {
        var result = service.stream(
                currentUserKeyResolver.resolve(), sessionId, request.toCommand(), idempotencyKey);
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            for (RolePlayStreamEvent event : result.events()) {
                emitter.send(SseEmitter.event().id(Long.toString(event.id()))
                        .name(event.type().eventName()).data(json(event)));
            }
            emitter.complete();
        } catch (IOException exception) {
            // The accepted turn and any completed provider result are already durable. Reconcile via GET /turns.
            emitter.complete();
        }
        return emitter;
    }

    @GetMapping("/turns")
    public RolePlayTurnPageResponse list(@PathVariable String sessionId) {
        return new RolePlayTurnPageResponse(service.listTurns(
                currentUserKeyResolver.resolve(), sessionId).stream().map(RolePlayTurnResponse::from).toList());
    }

    private String json(RolePlayStreamEvent event) {
        try {
            return objectMapper.writeValueAsString(event.data());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("role-play event could not be serialized", exception);
        }
    }
}
