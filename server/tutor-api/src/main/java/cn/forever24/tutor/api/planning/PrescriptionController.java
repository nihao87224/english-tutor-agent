package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.planning.PrescriptionApplicationService;
import cn.forever24.tutor.application.planning.PrescriptionMutationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionApplicationService service;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public PrescriptionController(
            PrescriptionApplicationService service,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.service = service;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @GetMapping("/today")
    public DailyLearningPrescriptionResponse today(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return DailyLearningPrescriptionResponse.from(
                service.getOrGenerateToday(currentUserKeyResolver.resolve(), date, timezone));
    }

    @PostMapping("/today/regenerations")
    public ResponseEntity<DailyLearningPrescriptionResponse> regenerate(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PrescriptionRegenerationRequest request
    ) {
        PrescriptionMutationResult result = service.regenerate(
                currentUserKeyResolver.resolve(), request.toCommand(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(DailyLearningPrescriptionResponse.from(result.prescription()));
    }

    @PostMapping("/{prescriptionId}/blocks/{blockId}/skips")
    public ResponseEntity<DailyLearningPrescriptionResponse> skip(
            @PathVariable String prescriptionId,
            @PathVariable String blockId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PrescriptionBlockSkipRequest request
    ) {
        PrescriptionMutationResult result = service.skipBlock(
                currentUserKeyResolver.resolve(),
                prescriptionId,
                blockId,
                request.reason(),
                request.note(),
                idempotencyKey);
        return ResponseEntity.ok()
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(DailyLearningPrescriptionResponse.from(result.prescription()));
    }
}
