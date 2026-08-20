package cn.forever24.tutor.infrastructure.curriculum;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CefrRange;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.CurriculumCatalog;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import cn.forever24.tutor.curriculum.DurationRange;
import cn.forever24.tutor.curriculum.EvidenceCriterion;
import cn.forever24.tutor.curriculum.MasteryImpactPolicy;
import cn.forever24.tutor.curriculum.Prerequisite;
import cn.forever24.tutor.curriculum.RetryPolicy;
import cn.forever24.tutor.curriculum.ReviewTemplate;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.SkillNode;
import cn.forever24.tutor.curriculum.SkillUnit;
import cn.forever24.tutor.curriculum.SkillUnitVariant;
import cn.forever24.tutor.curriculum.TrainingType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

final class CurriculumTestFixture {

    private CurriculumTestFixture() {
    }

    static CurriculumCatalog catalog(String suffix, CurriculumStatus targetSkillStatus, CurriculumStatus variantStatus) {
        String skillKey = "travel.confirm_information" + suffix;
        SkillNode root = new SkillNode(
                "travel.communication" + suffix,
                "Travel communication",
                "TRAVEL",
                null,
                new CefrRange(CefrLevel.A2, CefrLevel.B2),
                90,
                CurriculumStatus.ACTIVE);
        SkillNode target = new SkillNode(
                skillKey,
                "Confirm changed information",
                "TRAVEL",
                root.skillKey(),
                new CefrRange(CefrLevel.A2, CefrLevel.B2),
                85,
                targetSkillStatus);
        EvidenceCriterion criterion = new EvidenceCriterion(
                "confirm_new_gate",
                "Confirms the changed boarding gate accurately",
                BigDecimal.ONE,
                true,
                0);
        SkillUnitVariant variant = new SkillUnitVariant(
                "travel.confirm_gate_change.b1" + suffix,
                CefrLevel.B1,
                2,
                new DurationRange(10, 15),
                Set.of(TrainingType.GUIDED_SPEAKING, TrainingType.ROLE_PLAY),
                Set.of(ScaffoldingLevel.HIGH, ScaffoldingLevel.NONE),
                Set.of("missing_confirmation"),
                Set.of(skillKey),
                Set.of(),
                Set.of(new Prerequisite(root.skillKey(), new BigDecimal("0.45"), new BigDecimal("0.50"))),
                List.of(criterion),
                new CompletionPolicy(1, Set.of(criterion.criterionKey()), true),
                new RetryPolicy(true, 3, true),
                new MasteryImpactPolicy(true, false),
                variantStatus);
        SkillUnit unit = new SkillUnit(
                "travel.confirm_gate_change" + suffix,
                "Confirm changed travel information and ask what to do next",
                new ReviewTemplate("scheduled", TrainingType.REVIEW, "Recall confirmation and follow-up questions"),
                "1.0.0",
                CurriculumStatus.ACTIVE,
                List.of(variant));
        return new CurriculumCatalog(List.of(root, target), List.of(unit));
    }
}
