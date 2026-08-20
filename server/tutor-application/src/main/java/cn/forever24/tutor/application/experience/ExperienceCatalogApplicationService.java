package cn.forever24.tutor.application.experience;

import cn.forever24.tutor.experience.EpisodeMapping;
import cn.forever24.tutor.experience.EpisodeMappingResolver;
import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.ExperienceResolution;
import cn.forever24.tutor.experience.ExperienceResolutionRequest;
import cn.forever24.tutor.experience.MappingResourceReference;

public class ExperienceCatalogApplicationService {

    private final ExperienceRepository experienceRepository;
    private final ExperienceReferenceResolver referenceResolver;
    private final EpisodeMappingResolver resolver;

    public ExperienceCatalogApplicationService(
            ExperienceRepository experienceRepository,
            ExperienceReferenceResolver referenceResolver
    ) {
        this(experienceRepository, referenceResolver, new EpisodeMappingResolver());
    }

    ExperienceCatalogApplicationService(
            ExperienceRepository experienceRepository,
            ExperienceReferenceResolver referenceResolver,
            EpisodeMappingResolver resolver
    ) {
        if (experienceRepository == null || referenceResolver == null || resolver == null) {
            throw new IllegalArgumentException("experience dependencies are required");
        }
        this.experienceRepository = experienceRepository;
        this.referenceResolver = referenceResolver;
        this.resolver = resolver;
    }

    public void replaceCatalog(ExperienceCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        for (EpisodeMapping mapping : catalog.mappings()) {
            if (!referenceResolver.skillUnitVariantExists(mapping.skillUnitVariantKey())) {
                throw new IllegalArgumentException(
                        "unknown Skill Unit Variant reference: " + mapping.skillUnitVariantKey());
            }
            for (MappingResourceReference reference : mapping.resources()) {
                if (!referenceResolver.resourceVersionSupportsVariant(
                        reference.resourceKey(),
                        reference.resourceVersion(),
                        mapping.skillUnitVariantKey())) {
                    throw new IllegalArgumentException(
                            "unknown or incompatible resource version reference: "
                                    + reference.resourceKey() + "@" + reference.resourceVersion());
                }
            }
        }
        experienceRepository.replace(catalog);
    }

    public ExperienceResolution resolve(ExperienceResolutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("resolution request is required");
        }
        return experienceRepository.findCatalog()
                .map(catalog -> resolver.resolve(catalog, request))
                .orElseGet(() -> ExperienceResolution.noMapping("EXPERIENCE_CATALOG_EMPTY"));
    }
}
