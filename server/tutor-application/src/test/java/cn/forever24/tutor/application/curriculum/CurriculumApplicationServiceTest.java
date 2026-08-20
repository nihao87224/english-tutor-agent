package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumCatalog;
import cn.forever24.tutor.curriculum.SkillNode;
import cn.forever24.tutor.curriculum.SkillUnitVariant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurriculumApplicationServiceTest {

    @Test
    void queriesOnlyThroughTheCurriculumPort() {
        RecordingRepository repository = new RecordingRepository();
        CurriculumApplicationService service = new CurriculumApplicationService(repository);

        assertEquals(List.of(), service.findActiveVariants(CefrLevel.B1, "travel.confirm_information"));
        assertEquals(CefrLevel.B1, repository.query.level());
        assertEquals("travel.confirm_information", repository.query.skillKey());
    }

    @Test
    void rejectsMissingImportCatalogBeforeCallingTheRepository() {
        CurriculumApplicationService service = new CurriculumApplicationService(new RecordingRepository());

        assertThrows(IllegalArgumentException.class, () -> service.importCatalog(null));
    }

    private static final class RecordingRepository implements CurriculumRepository {
        private CurriculumVariantQuery query;

        @Override
        public void replace(CurriculumCatalog catalog) {
            // No persistence is needed for this application-boundary test.
        }

        @Override
        public Optional<SkillNode> findSkill(String skillKey) {
            return Optional.empty();
        }

        @Override
        public List<SkillNode> findSkills() {
            return List.of();
        }

        @Override
        public List<SkillUnitVariant> findVariants(CurriculumVariantQuery query) {
            this.query = query;
            return List.of();
        }
    }
}
