package cn.forever24.tutor.api.conversation;

import cn.forever24.tutor.application.conversation.ConversationApplicationService;
import cn.forever24.tutor.application.conversation.ConversationMessageType;
import cn.forever24.tutor.application.conversation.ConversationStreamEvent;
import cn.forever24.tutor.application.conversation.ConversationStreamRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ConversationController {

    private static final String DEFAULT_USER_KEY = "local-dev-user";

    private final ConversationApplicationService conversationApplicationService;

    public ConversationController(ConversationApplicationService conversationApplicationService) {
        this.conversationApplicationService = conversationApplicationService;
    }

    @PostMapping(
            value = "/conversations/{sessionId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter streamMessage(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("sessionId") String sessionId,
            @RequestBody ConversationMessageRequest request
    ) {
        List<ConversationStreamEvent> events = conversationApplicationService.streamMessage(
                new ConversationStreamRequest(
                        resolveUserKey(userKey),
                        sessionId,
                        parseMessageType(request == null ? null : request.messageType()),
                        request == null ? null : request.text(),
                        request == null ? null : request.taskId(),
                        idempotencyKey));
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            for (ConversationStreamEvent event : events) {
                emitter.send(SseEmitter.event()
                        .id(Long.toString(event.id()))
                        .name(event.type().eventName())
                        .data(serialize(event)));
            }
            emitter.complete();
        } catch (RuntimeException | IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid conversation stream request");
        return ResponseEntity.badRequest().body(problem);
    }

    private String serialize(ConversationStreamEvent event) {
        return event.data().entrySet().stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":" + jsonValue(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String jsonValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> "\"" + escape(String.valueOf(entry.getKey())) + "\":" + jsonValue(entry.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(",");
                }
                builder.append(jsonValue(item));
                first = false;
            }
            return builder.append("]").toString();
        }
        return "\"" + escape(value == null ? "" : value.toString()) + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private ConversationMessageType parseMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("messageType is required");
        }
        return ConversationMessageType.valueOf(messageType);
    }

    private String resolveUserKey(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return DEFAULT_USER_KEY;
        }
        return userKey;
    }
}
