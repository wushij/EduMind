package com.edumind;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Module boundary gates: AI and statistics must not depend on other domains' implementation layers.
 */
class ModuleBoundaryArchTest {

    private static JavaClasses classes;

    private static final String[] FOREIGN_IMPLEMENTATION_PACKAGES = {
            "com.edumind.course.entity..",
            "com.edumind.course.mapper..",
            "com.edumind.course.service..",
            "com.edumind.course.dao..",
            "com.edumind.question.entity..",
            "com.edumind.question.mapper..",
            "com.edumind.question.service..",
            "com.edumind.question.dao..",
            "com.edumind.knowledge.entity..",
            "com.edumind.knowledge.mapper..",
            "com.edumind.knowledge.service..",
            "com.edumind.knowledge.dao..",
            "com.edumind.teaching.entity..",
            "com.edumind.teaching.mapper..",
            "com.edumind.teaching.service..",
            "com.edumind.teaching.dao..",
            "com.edumind.resource.entity..",
            "com.edumind.resource.mapper..",
            "com.edumind.resource.service..",
            "com.edumind.resource.dao..",
            "com.edumind.notification.entity..",
            "com.edumind.notification.mapper..",
            "com.edumind.notification.service..",
            "com.edumind.notification.dao..",
            "com.edumind.system.entity..",
            "com.edumind.system.mapper..",
            "com.edumind.system.service..",
            "com.edumind.system.dao.."
    };

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edumind");
    }

    @Test
    void aiModuleMustNotDependOnForeignImplementationLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edumind.ai..")
                .should().dependOnClassesThat().resideInAnyPackage(FOREIGN_IMPLEMENTATION_PACKAGES);
        rule.check(classes);
    }

    @Test
    void statisticsModuleMustNotDependOnForeignImplementationLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edumind.statistics..")
                .should().dependOnClassesThat().resideInAnyPackage(FOREIGN_IMPLEMENTATION_PACKAGES);
        rule.check(classes);
    }

    @Test
    void commonMustNotHostCrossDomainApiFacades() {
        var violating = classes.stream()
                .filter(jc -> jc.getPackageName().startsWith("com.edumind.common.api"))
                .filter(jc -> jc.isInterface() && jc.getSimpleName().endsWith("Api"))
                .toList();
        Assertions.assertTrue(
                violating.isEmpty(),
                () -> "Business facade APIs must not live in edu-mind-common: " + violating);
    }

    private static final String[] API_CONTRACT_PACKAGES = {
            "com.edumind.system.api..",
            "com.edumind.system.vo..",
            "com.edumind.course.api..",
            "com.edumind.course.vo..",
            "com.edumind.question.api..",
            "com.edumind.question.vo..",
            "com.edumind.question.dto..",
            "com.edumind.resource.api..",
            "com.edumind.resource.vo..",
            "com.edumind.notification.api..",
            "com.edumind.statistics.api..",
            "com.edumind.knowledge.api..",
            "com.edumind.knowledge.vo..",
            "com.edumind.teaching.api..",
            "com.edumind.teaching.vo..",
            "com.edumind.ai.api..",
            "com.edumind.ai.vo..",
            "com.edumind.ai.dto.."
    };

    @Test
    void apiContractArtifactsMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(API_CONTRACT_PACKAGES)
                .and().resideOutsideOfPackages("..api.impl..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
        rule.check(classes);
    }

    @Test
    void businessModulesMustNotDeclarePublicApiInterfaces() {
        var violating = classes.stream()
                .filter(jc -> jc.isInterface())
                .filter(jc -> jc.getSimpleName().endsWith("Api"))
                .filter(jc -> {
                    String pkg = jc.getPackageName();
                    if (!pkg.matches("com\\.edumind\\.[a-z]+\\.api(\\..+)?")) {
                        return false;
                    }
                    return !pkg.contains(".api.impl.");
                })
                .filter(jc -> jc.getSource()
                        .map(s -> s.toString().contains("edu-mind-modules"))
                        .orElse(false))
                .toList();
        Assertions.assertTrue(
                violating.isEmpty(),
                () -> "Cross-module API interfaces belong in edu-mind-api jars, not business modules: " + violating);
    }
}
