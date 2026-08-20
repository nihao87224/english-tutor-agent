package cn.forever24.tutor.application.experience;

public interface ExperienceReferenceResolver {

    boolean skillUnitVariantExists(String skillUnitVariantKey);

    boolean resourceVersionSupportsVariant(
            String resourceKey,
            String resourceVersion,
            String skillUnitVariantKey
    );
}
