package cn.forever24.tutor.planning;

import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record DailyLearningPrescription(
        String prescriptionId,
        UserKey userKey,
        LocalDate learningDate,
        ZoneId learnerZone,
        long version,
        PrescriptionStatus status,
        PrescriptionGoal priorityGoal,
        List<PrescriptionBlock> blocks,
        String rationale,
        List<String> reasonCodes,
        PedagogicalPolicyVersion policyVersion,
        LearnerInputSnapshot inputSnapshot,
        Instant generatedAt,
        Instant expiresAt,
        String supersedesPrescriptionId
) {

    public DailyLearningPrescription {
        if (prescriptionId == null || prescriptionId.isBlank() || prescriptionId.strip().length() > 64) {
            throw new IllegalArgumentException("valid prescriptionId is required");
        }
        prescriptionId = prescriptionId.strip();
        if (userKey == null || learningDate == null || learnerZone == null || version < 1
                || status == null || priorityGoal == null || policyVersion == null
                || inputSnapshot == null || generatedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("prescription metadata is required");
        }
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("expiresAt must be after generatedAt");
        }
        if (rationale == null || rationale.isBlank() || rationale.strip().length() > 1000) {
            throw new IllegalArgumentException("valid rationale is required");
        }
        rationale = rationale.strip();
        if (reasonCodes == null || reasonCodes.isEmpty()
                || reasonCodes.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("reasonCodes must not be empty");
        }
        reasonCodes = reasonCodes.stream().map(String::strip).distinct().toList();
        if (blocks == null || blocks.isEmpty() || blocks.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("prescription blocks must not be empty");
        }
        blocks = blocks.stream().sorted(Comparator.comparingInt(PrescriptionBlock::sequence)).toList();
        if (blocks.stream().map(PrescriptionBlock::blockId).distinct().count() != blocks.size()
                || blocks.stream().map(PrescriptionBlock::sequence).distinct().count() != blocks.size()) {
            throw new IllegalArgumentException("prescription block ids and sequences must be unique");
        }
        if (status == PrescriptionStatus.ACTIVE && blocks.stream().noneMatch(block ->
                block.status() == PrescriptionBlockStatus.READY && block.type() == BlockType.OUTPUT)) {
            throw new IllegalArgumentException("active prescription requires a ready OUTPUT block");
        }
        int totalMinutes = blocks.stream()
                .filter(block -> block.status() == PrescriptionBlockStatus.READY)
                .mapToInt(PrescriptionBlock::estimatedMinutes)
                .sum();
        if (totalMinutes > inputSnapshot.availableMinutes()) {
            throw new IllegalArgumentException("prescription exceeds available time");
        }
        if (supersedesPrescriptionId != null && supersedesPrescriptionId.isBlank()) {
            throw new IllegalArgumentException("supersedesPrescriptionId must not be blank");
        }
    }

    public int estimatedMinutes() {
        return blocks.stream()
                .filter(block -> block.status() == PrescriptionBlockStatus.READY)
                .mapToInt(PrescriptionBlock::estimatedMinutes)
                .sum();
    }

    public DailyLearningPrescription superseded() {
        return new DailyLearningPrescription(
                prescriptionId, userKey, learningDate, learnerZone, version,
                PrescriptionStatus.SUPERSEDED, priorityGoal, blocks, rationale, reasonCodes,
                policyVersion, inputSnapshot, generatedAt, expiresAt, supersedesPrescriptionId);
    }

    public DailyLearningPrescription skipBlock(String blockId) {
        PrescriptionBlock target = blocks.stream()
                .filter(block -> block.blockId().equals(blockId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("prescription block was not found"));
        if (target.status() == PrescriptionBlockStatus.SKIPPED) {
            return this;
        }
        long otherOutputs = blocks.stream()
                .filter(block -> !block.blockId().equals(blockId))
                .filter(block -> block.status() == PrescriptionBlockStatus.READY)
                .filter(block -> block.type() == BlockType.OUTPUT)
                .count();
        if (target.type() == BlockType.OUTPUT && otherOutputs == 0) {
            throw new IllegalStateException("the only OUTPUT block cannot be skipped");
        }
        List<PrescriptionBlock> changed = new ArrayList<>(blocks.size());
        for (PrescriptionBlock block : blocks) {
            changed.add(block.blockId().equals(blockId) ? block.skipped() : block);
        }
        return new DailyLearningPrescription(
                prescriptionId, userKey, learningDate, learnerZone, version, status,
                priorityGoal, changed, rationale, reasonCodes, policyVersion, inputSnapshot,
                generatedAt, expiresAt, supersedesPrescriptionId);
    }
}
