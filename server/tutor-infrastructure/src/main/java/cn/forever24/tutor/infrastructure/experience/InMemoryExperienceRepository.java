package cn.forever24.tutor.infrastructure.experience;

import cn.forever24.tutor.application.experience.ExperienceRepository;
import cn.forever24.tutor.experience.ExperienceCatalog;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryExperienceRepository implements ExperienceRepository {

    private final AtomicReference<ExperienceCatalog> catalog = new AtomicReference<>();

    @Override
    public void replace(ExperienceCatalog replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        catalog.set(replacement);
    }

    @Override
    public Optional<ExperienceCatalog> findCatalog() {
        return Optional.ofNullable(catalog.get());
    }
}
