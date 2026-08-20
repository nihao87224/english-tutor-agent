package cn.forever24.tutor.infrastructure.experience;

import cn.forever24.tutor.application.curriculum.CurriculumRepository;
import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.application.experience.ExperienceReferenceResolver;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.curriculum.CurriculumStatus;

public class RepositoryBackedExperienceReferenceResolver implements ExperienceReferenceResolver {

    private final CurriculumRepository curriculumRepository;
    private final ResourceCatalogRepository resourceCatalogRepository;

    public RepositoryBackedExperienceReferenceResolver(
            CurriculumRepository curriculumRepository,
            ResourceCatalogRepository resourceCatalogRepository
    ) {
        this.curriculumRepository = curriculumRepository;
        this.resourceCatalogRepository = resourceCatalogRepository;
    }

    @Override
    public boolean skillUnitVariantExists(String skillUnitVariantKey) {
        return curriculumRepository.findVariants(new CurriculumVariantQuery(
                        null, null, CurriculumStatus.ACTIVE)).stream()
                .anyMatch(variant -> variant.variantKey().equals(skillUnitVariantKey));
    }

    @Override
    public boolean resourceVersionSupportsVariant(
            String resourceKey,
            String resourceVersion,
            String skillUnitVariantKey
    ) {
        return resourceCatalogRepository.findExactVersion(resourceKey, resourceVersion)
                .map(entry -> entry.resourceVersion().skillUnitVariantKeys().contains(skillUnitVariantKey))
                .orElse(false);
    }
}
