package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.planning.PrescriptionApplicationException;
import cn.forever24.tutor.application.planning.PrescriptionApplicationService;
import cn.forever24.tutor.application.planning.PrescriptionMutationResult;
import cn.forever24.tutor.application.planning.PrescriptionFeedbackReason;
import cn.forever24.tutor.application.planning.RegeneratePrescriptionCommand;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.LearnerInputSnapshot;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionGoal;
import cn.forever24.tutor.planning.PrescriptionResourceRef;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.PrescriptionTaskHero;
import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionControllerTest {

    @Test
    void todayMapsPersistedResourceVersionEvidenceAndLinMuenSceneHero() throws Exception {
        PrescriptionApplicationService service = mock(PrescriptionApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        when(service.getOrGenerateToday("usr-1", LocalDate.of(2026, 8, 20), "Asia/Shanghai"))
                .thenReturn(prescription());
        PrescriptionController controller = new PrescriptionController(service, resolver);

        DailyLearningPrescriptionResponse response = controller.today(
                LocalDate.of(2026, 8, 20), "Asia/Shanghai");

        assertEquals("1.0.0", response.blocks().getFirst().resource().resourceVersion());
        assertEquals(List.of("confirm_information"), response.blocks().getFirst().expectedEvidence());
        assertTrue(response.blocks().getFirst().taskHero().altText().contains("Lin Muen"));
        assertEquals("S01", response.experience().seasonId());
        assertEquals("/today", PrescriptionController.class.getMethod(
                "today", LocalDate.class, String.class).getAnnotation(GetMapping.class).value()[0]);
    }

    @Test
    void regenerationReturnsCreatedAndReplayHeader() throws Exception {
        PrescriptionApplicationService service = mock(PrescriptionApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        when(service.regenerate(anyString(), any(), anyString()))
                .thenReturn(new PrescriptionMutationResult(prescription(), true));
        PrescriptionController controller = new PrescriptionController(service, resolver);
        PrescriptionRegenerationRequest request = new PrescriptionRegenerationRequest(
                "prescription-1", 1, PrescriptionFeedbackReason.TIME_INSUFFICIENT,
                5, null, null);

        ResponseEntity<DailyLearningPrescriptionResponse> response = controller.regenerate("idem-1", request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("true", response.getHeaders().getFirst("Idempotency-Replayed"));
        verify(service).regenerate("usr-1", new RegeneratePrescriptionCommand(
                "prescription-1", 1, PrescriptionFeedbackReason.TIME_INSUFFICIENT,
                5, null, null), "idem-1");
        assertEquals("/today/regenerations", PrescriptionController.class.getMethod(
                        "regenerate", String.class, PrescriptionRegenerationRequest.class)
                .getAnnotation(PostMapping.class).value()[0]);
    }

    @Test
    void noCandidateProblemIncludesFallbackAvailability() {
        PrescriptionExceptionHandler handler = new PrescriptionExceptionHandler();

        ResponseEntity<?> response = handler.handlePrescription(
                PrescriptionApplicationException.noCandidate(true));

        assertEquals(409, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("fallbackAvailable=true"));
    }

    private static DailyLearningPrescription prescription() {
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        PrescriptionSkillState state = new PrescriptionSkillState(
                "speaking", new BigDecimal("0.35"), new BigDecimal("0.75"),
                CefrLevel.A2, 3, now.minusSeconds(3600));
        PrescriptionBlock block = new PrescriptionBlock(
                "block-1", 1, BlockType.OUTPUT, "Airport gate confirmation",
                "travel.confirm-information.a2",
                new PrescriptionResourceRef("season1.ep006.gate-change.a2", "1.0.0"),
                "s01.ep006.gate-change.a2", "S01", "EP006", "GATE_CHANGE",
                CefrLevel.A2, ScaffoldingLevel.HIGH, TrainingType.ROLE_PLAY, 5,
                List.of("confirm_information"),
                new CompletionPolicy(1, Set.of("confirm_information"), true),
                null,
                Map.of("SKILL_GAP", new BigDecimal("0.65")),
                new PrescriptionTaskHero(
                        "asset-task-hero-airport", "https://cdn.example.invalid/hero.webp", "16:9",
                        new BigDecimal("0.68"), new BigDecimal("0.42"),
                        "Lin Muen stands full body beside the airport gate."),
                PrescriptionBlockStatus.READY);
        return new DailyLearningPrescription(
                "prescription-1", new UserKey("usr-1"), LocalDate.of(2026, 8, 20),
                ZoneId.of("Asia/Shanghai"), 1, PrescriptionStatus.ACTIVE,
                new PrescriptionGoal("GENERAL_COMMUNICATION", "综合英语沟通"), List.of(block),
                "今天优先训练机场信息确认。", List.of("GOAL_MATCH", "SKILL_GAP"),
                PedagogicalPolicyVersion.V2_P0_1,
                new LearnerInputSnapshot(7, 20, "GENERAL", null, List.of(state)),
                now, now.plusSeconds(86_400), null);
    }
}
