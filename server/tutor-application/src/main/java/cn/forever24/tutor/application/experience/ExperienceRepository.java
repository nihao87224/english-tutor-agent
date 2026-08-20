package cn.forever24.tutor.application.experience;

import cn.forever24.tutor.experience.ExperienceCatalog;

import java.util.Optional;

public interface ExperienceRepository {

    void replace(ExperienceCatalog catalog);

    Optional<ExperienceCatalog> findCatalog();
}
