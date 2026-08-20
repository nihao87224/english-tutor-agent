package cn.forever24.tutor.curriculum;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurriculumCatalogTest {

    @Test
    void acceptsAValidMinimalCurriculum() {
        CurriculumCatalog catalog = catalog(skills(), List.of(unit("travel.confirm_gate_change.b1", "travel.confirm_information")));

        assertEquals(2, catalog.skills().size());
        assertEquals("travel.confirm_gate_change.b1", catalog.skillUnits().getFirst().variants().getFirst().variantKey());
    }

    @Test
    void rejectsDuplicateKeysAcrossTheImportedCatalog() {
        List<SkillNode> duplicatedSkills = List.of(skills().getFirst(), skills().getFirst());
        assertThrows(IllegalArgumentException.class, () -> catalog(
                duplicatedSkills,
                List.of(unit("travel.confirm_gate_change.b1", "travel.confirm_information"))));

        SkillUnit first = unit("travel.confirm_gate_change.b1", "travel.confirm_information");
        SkillUnit second = new SkillUnit(
                "travel.confirm_gate_change.alternative",
                "Confirm changed travel information in a second context",
                first.reviewTemplate(),
                "1.0.0",
                CurriculumStatus.ACTIVE,
                first.variants());
        assertThrows(IllegalArgumentException.class, () -> catalog(skills(), List.of(first, second)));
    }

    @Test
    void rejectsUnknownPrerequisite() {
        SkillUnit invalid = unit("travel.confirm_gate_change.b1", "travel.missing_skill");

        assertThrows(IllegalArgumentException.class, () -> catalog(skills(), List.of(invalid)));
    }

    @Test
    void rejectsSkillParentCycle() {
        List<SkillNode> cyclic = List.of(
                skill("travel.communication", "travel.confirm_information", CurriculumStatus.ACTIVE),
                skill("travel.confirm_information", "travel.communication", CurriculumStatus.ACTIVE));

        assertThrows(IllegalArgumentException.class, () -> catalog(
                cyclic,
                List.of(unit("travel.confirm_gate_change.b1", "travel.confirm_information"))));
    }

    @Test
    void completionCannotBeConfiguredAsMastery() {
        assertThrows(IllegalArgumentException.class, () -> new CompletionPolicy(
                1,
                Set.of("confirm_new_gate"),
                false));
        assertThrows(IllegalArgumentException.class, () -> new MasteryImpactPolicy(true, true));
    }

    private static CurriculumCatalog catalog(List<SkillNode> skills, List<SkillUnit> units) {
        return new CurriculumCatalog(skills, units);
    }

    private static List<SkillNode> skills() {
        return List.of(
                skill("travel.communication", null, CurriculumStatus.ACTIVE),
                skill("travel.confirm_information", "travel.communication", CurriculumStatus.ACTIVE));
    }

    private static SkillNode skill(String key, String parent, CurriculumStatus status) {
        return new SkillNode(
                key,
                key,
                "TRAVEL",
                parent,
                new CefrRange(CefrLevel.A2, CefrLevel.B2),
                80,
                status);
    }

    private static SkillUnit unit(String variantKey, String prerequisiteSkill) {
        EvidenceCriterion criterion = new EvidenceCriterion(
                "confirm_new_gate",
                "Confirms the changed boarding gate accurately",
                BigDecimal.ONE,
                true,
                0);
        SkillUnitVariant variant = new SkillUnitVariant(
                variantKey,
                CefrLevel.B1,
                2,
                new DurationRange(10, 15),
                Set.of(TrainingType.GUIDED_SPEAKING, TrainingType.ROLE_PLAY),
                Set.of(ScaffoldingLevel.HIGH, ScaffoldingLevel.NONE),
                Set.of("missing_confirmation"),
                Set.of("travel.confirm_information"),
                Set.of(),
                Set.of(new Prerequisite(prerequisiteSkill, new BigDecimal("0.45"), new BigDecimal("0.50"))),
                List.of(criterion),
                new CompletionPolicy(1, Set.of(criterion.criterionKey()), true),
                new RetryPolicy(true, 3, true),
                new MasteryImpactPolicy(true, false),
                CurriculumStatus.ACTIVE);
        return new SkillUnit(
                "travel.confirm_gate_change",
                "Confirm changed travel information and ask what to do next",
                new ReviewTemplate("scheduled", TrainingType.REVIEW, "Recall confirmation and follow-up questions"),
                "1.0.0",
                CurriculumStatus.ACTIVE,
                List.of(variant));
    }
}
