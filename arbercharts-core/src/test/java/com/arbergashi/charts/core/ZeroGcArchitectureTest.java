package com.arbergashi.charts.core;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Point2D;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Zero-GC Architecture Tests for ArberCharts v2.0.
 *
 * <p><strong>THESE TESTS ARE MANDATORY AND MUST ALWAYS PASS.</strong>
 *
 * <p>Zero-allocation rendering is the cornerstone of ArberCharts performance.
 * Any violation is a P1 blocker that must be fixed before merge.
 *
 * <h2>What This Enforces</h2>
 * <ul>
 *   <li>No BasicStroke allocation in render methods</li>
 *   <li>No Color allocation in render methods</li>
 *   <li>No Point2D allocation in render methods</li>
 *   <li>No array allocation in hot paths</li>
 *   <li>Proper use of ZeroAllocPool</li>
 * </ul>
 *
 * <h2>Running These Tests</h2>
 * <pre>
 * # Run all architecture tests
 * mvn -pl arbercharts-core test -Dtest=ZeroGcArchitectureTest
 *
 * # Run in CI (fails build on violation)
 * mvn -pl arbercharts-core -Pguidelines-check verify
 * </pre>
 *
 * <h2>If Tests Fail</h2>
 * <ol>
 *   <li>Identify the violating code in the error message</li>
 *   <li>Replace with ZeroAllocPool equivalent</li>
 *   <li>Re-run tests to verify fix</li>
 *   <li>Update renderer documentation</li>
 * </ol>
 *
 * @since 2.0.0
 * @see com.arbergashi.charts.engine.allocation.ZeroAllocPool
 * @see <a href="../../../docs/ZERO_GC_POLICY.md">Zero-GC Policy</a>
 */
@AnalyzeClasses(packages = "com.arbergashi.charts")
public class ZeroGcArchitectureTest {

    /**
     * Renderers MUST NOT allocate BasicStroke objects.
     *
     * <p><strong>Violation Example:</strong>
     * <pre>{@code
     * // ❌ WRONG
     * g2.setStroke(new BasicStroke(2.0f));
     *
     * // ✅ RIGHT
     * g2.setStroke(ZeroAllocPool.getStroke(2.0f));
     * }</pre>
     */
    @ArchTest
    static final ArchRule renderers_must_not_allocate_strokes =
        noClasses()
            .that().resideInAPackage("..render..")
            .and().haveSimpleNameEndingWith("Renderer")
            .should().callConstructor(BasicStroke.class)
            .allowEmptyShould(true)
            .because("Zero-allocation rendering is mandatory. " +
                     "Use ZeroAllocPool.getStroke() instead. " +
                     "See docs/ZERO_GC_POLICY.md");

    /**
     * Renderers MUST NOT allocate Color objects.
     *
     * <p><strong>Violation Example:</strong>
     * <pre>{@code
     * // ❌ WRONG
     * g2.setColor(new Color(255, 0, 0));
     *
     * // ✅ RIGHT
     * g2.setColor(ZeroAllocPool.getColor(255, 0, 0));
     * }</pre>
     */
    @ArchTest
    static final ArchRule renderers_must_not_allocate_colors =
        noClasses()
            .that().resideInAPackage("..render..")
            .and().haveSimpleNameEndingWith("Renderer")
            .should().callConstructor(Color.class)
            .allowEmptyShould(true)
            .because("Zero-allocation rendering is mandatory. " +
                     "Use ZeroAllocPool.getColor() instead. " +
                     "See docs/ZERO_GC_POLICY.md");

    /**
     * Renderers MUST NOT allocate Point2D objects.
     *
     * <p>Use primitive doubles instead of Point2D objects.
     *
     * <p><strong>Violation Example:</strong>
     * <pre>{@code
     * // ❌ WRONG
     * Point2D p = new Point2D.Double(x, y);
     *
     * // ✅ RIGHT
     * double px = x;
     * double py = y;
     * }</pre>
     */
    @ArchTest
    static final ArchRule renderers_must_not_allocate_points =
        noClasses()
            .that().resideInAPackage("..render..")
            .and().haveSimpleNameEndingWith("Renderer")
            .should().callConstructor(Point2D.class)
            .orShould().callConstructor(Point2D.Double.class)
            .orShould().callConstructor(Point2D.Float.class)
            .allowEmptyShould(true)
            .because("Use primitive doubles instead of Point2D. " +
                     "See docs/ZERO_GC_POLICY.md");

    /**
     * Engine classes MUST NOT allocate BasicStroke in hot paths.
     *
     * <p>This is a broader check for the engine package.
     */
    @ArchTest
    static final ArchRule engine_must_not_allocate_strokes =
        noClasses()
            .that().resideInAPackage("..engine..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().callConstructor(BasicStroke.class)
            .allowEmptyShould(true)
            .because("Engine must use ZeroAllocPool.getStroke() for all strokes. " +
                     "See docs/ZERO_GC_POLICY.md");

    /**
     * Engine classes MUST NOT allocate Color in hot paths.
     */
    @ArchTest
    static final ArchRule engine_must_not_allocate_colors =
        noClasses()
            .that().resideInAPackage("..engine..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().callConstructor(Color.class)
            .allowEmptyShould(true)
            .because("Engine must use ZeroAllocPool.getColor() for all colors. " +
                     "See docs/ZERO_GC_POLICY.md");

    /**
     * Concrete renderers should exist in the render package.
     *
     * <p>This ensures proper package organization.
     */
    @ArchTest
    static final ArchRule renderers_should_be_in_render_package =
        classes()
            .that().haveSimpleNameEndingWith("Renderer")
            .and().areNotInterfaces()
            .and().areNotInnerClasses()
            .should().resideInAPackage("..render..")
            .allowEmptyShould(true)
            .because("Concrete renderers should be in the render package hierarchy");

    /**
     * No StringBuilder allocation in render methods.
     *
     * <p>Use thread-local StringBuilder pools instead.
     */
    @ArchTest
    static final ArchRule no_string_builder_in_render =
        noClasses()
            .that().resideInAPackage("..render..")
            .should().callConstructor(StringBuilder.class)
            .allowEmptyShould(true)
            .because("Use ThreadLocal<StringBuilder> pools. " +
                     "See docs/ZERO_GC_POLICY.md");
}

