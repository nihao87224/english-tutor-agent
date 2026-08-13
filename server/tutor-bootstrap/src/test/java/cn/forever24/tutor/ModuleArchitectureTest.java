package cn.forever24.tutor;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleArchitectureTest {

    private static JavaClasses classes;
    private static final String API_PACKAGE = "cn.forever24.tutor.api..";
    private static final String APPLICATION_PACKAGE = "cn.forever24.tutor.application..";
    private static final String[] DOMAIN_PACKAGES = {
            "cn.forever24.tutor.assessment..",
            "cn.forever24.tutor.shared..",
            "cn.forever24.tutor.profile..",
            "cn.forever24.tutor.planning..",
            "cn.forever24.tutor.training..",
            "cn.forever24.tutor.learner..",
            "cn.forever24.tutor.reporting.."
    };
    private static final String AGENT_PACKAGE = "cn.forever24.tutor.ai..";
    private static final String INFRASTRUCTURE_PACKAGE = "cn.forever24.tutor.infrastructure..";
    private static final String OBSERVABILITY_PACKAGE = "cn.forever24.tutor.observability..";

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cn.forever24.tutor");
    }

    @Test
    void domainPackagesDoNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.springframework.ai.."
                );

        rule.check(classes);
    }

    @Test
    void domainPackagesDoNotDependOnOuterModules() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        APPLICATION_PACKAGE,
                        AGENT_PACKAGE,
                        INFRASTRUCTURE_PACKAGE,
                        OBSERVABILITY_PACKAGE
                );

        rule.check(classes);
    }

    @Test
    void applicationDoesNotDependOnOuterModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(APPLICATION_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        AGENT_PACKAGE,
                        INFRASTRUCTURE_PACKAGE,
                        OBSERVABILITY_PACKAGE
                );

        rule.check(classes);
    }

    @Test
    void apiDoesNotDependOnAdaptersOrObservability() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(API_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        AGENT_PACKAGE,
                        INFRASTRUCTURE_PACKAGE,
                        OBSERVABILITY_PACKAGE
                );

        rule.check(classes);
    }

    @Test
    void infrastructureDoesNotDependOnApiAgentOrObservability() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(INFRASTRUCTURE_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        AGENT_PACKAGE,
                        OBSERVABILITY_PACKAGE
                );

        rule.check(classes);
    }

    @Test
    void agentDoesNotDependOnApiInfrastructureOrObservability() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(AGENT_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        INFRASTRUCTURE_PACKAGE,
                        OBSERVABILITY_PACKAGE
                );

        rule.check(classes);
    }

    @Test
    void observabilityDoesNotDependOnBusinessModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(OBSERVABILITY_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        APPLICATION_PACKAGE,
                        DOMAIN_PACKAGES[0],
                        DOMAIN_PACKAGES[1],
                        DOMAIN_PACKAGES[2],
                        AGENT_PACKAGE,
                        INFRASTRUCTURE_PACKAGE
                );

        rule.check(classes);
    }
}
