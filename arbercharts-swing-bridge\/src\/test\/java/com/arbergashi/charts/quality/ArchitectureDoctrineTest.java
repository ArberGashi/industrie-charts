package com.arbergashi.charts.quality;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;

@AnalyzeClasses(
        packages = "com.arbergashi.charts",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
class ArchitectureDoctrineTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule domain_must_be_pure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..platform..", "..engine..", "..render..", "..javax.swing..", "..java.awt..")
                    .allowEmptyShould(true);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule domain_render_must_not_depend_on_platform =
            noClasses()
                    .that().resideInAPackage("..domain.render..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..platform..", "..engine..")
                    .allowEmptyShould(true);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule platform_must_not_depend_on_engine =
            noClasses()
                    .that().resideInAPackage("..platform..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..engine..")
                    .allowEmptyShould(true);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule render_must_not_depend_on_platform_export =
            noClasses()
                    .that().resideInAPackage("..render..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..platform.export..")
                    .allowEmptyShould(true);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule engine_must_be_headless =
            noClasses()
                    .that().resideInAPackage("..engine..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..javax.swing..", "..java.awt..")
                    .allowEmptyShould(true);
}
