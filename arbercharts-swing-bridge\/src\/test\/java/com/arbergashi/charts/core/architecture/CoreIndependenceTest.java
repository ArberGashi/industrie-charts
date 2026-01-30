package com.arbergashi.charts.core.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.arbergashi.charts.core")
public class CoreIndependenceTest {

    @ArchTest
    static final ArchRule core_must_be_awt_free = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.awt..",
                    "javax.swing..",
                    "org.jetbrains.skia..",
                    "androidx.compose.."
            )
            .because("The ArberCharts Core must be 100% headless and platform-independent.");
}
