package cn.forever24.tutor.infrastructure.curriculum;

import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCurriculumRepositoryTest {

    @Test
    void findsActiveVariantByLevelAndSkill() {
        InMemoryCurriculumRepository repository = new InMemoryCurriculumRepository();
        repository.replace(CurriculumTestFixture.catalog("", CurriculumStatus.ACTIVE, CurriculumStatus.ACTIVE));

        var variants = repository.findVariants(CurriculumVariantQuery.active(
                CefrLevel.B1,
                "travel.confirm_information"));

        assertEquals(1, variants.size());
        assertEquals("travel.confirm_gate_change.b1", variants.getFirst().variantKey());
        assertTrue(repository.findVariants(CurriculumVariantQuery.active(CefrLevel.A2, null)).isEmpty());
    }

    @Test
    void disabledTargetSkillDoesNotProduceAnActiveCandidate() {
        InMemoryCurriculumRepository repository = new InMemoryCurriculumRepository();
        repository.replace(CurriculumTestFixture.catalog("", CurriculumStatus.DISABLED, CurriculumStatus.ACTIVE));

        assertTrue(repository.findVariants(CurriculumVariantQuery.active(CefrLevel.B1, null)).isEmpty());
    }

    @Test
    void statusFilterExcludesDisabledVariantFromActiveQuery() {
        InMemoryCurriculumRepository repository = new InMemoryCurriculumRepository();
        repository.replace(CurriculumTestFixture.catalog("", CurriculumStatus.ACTIVE, CurriculumStatus.DISABLED));

        assertTrue(repository.findVariants(CurriculumVariantQuery.active(CefrLevel.B1, null)).isEmpty());
        assertEquals(1, repository.findVariants(new CurriculumVariantQuery(
                CefrLevel.B1,
                null,
                CurriculumStatus.DISABLED)).size());
    }
}
