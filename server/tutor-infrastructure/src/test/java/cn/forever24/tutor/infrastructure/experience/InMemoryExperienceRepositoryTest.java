package cn.forever24.tutor.infrastructure.experience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryExperienceRepositoryTest {

    @Test
    void replacesAndReadsSeasonOneFixture() {
        InMemoryExperienceRepository repository = new InMemoryExperienceRepository();

        repository.replace(SeasonOneExperienceFixture.catalog());

        var stored = repository.findCatalog().orElseThrow();
        assertEquals(3, stored.episodes().size());
        assertEquals(5, stored.mappings().size());
        assertTrue(stored.episodes().stream().noneMatch(episode -> episode.storyOrderRequired()));
    }
}
